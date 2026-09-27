package com.court.mediation.config;

import com.court.mediation.entity.*;
import com.court.mediation.repo.*;
import com.court.mediation.security.CurrentUser;
import com.court.mediation.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 演示数据初始化：创建五类角色账号与覆盖全生命周期的案件，
 * 包含分期履行、部分履行、逾期、失联、缺料、多次改期、转立案、司法确认等典型场景。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final CaseService caseService;
    private final ScheduleService scheduleService;
    private final MediationService mediationService;
    private final AgreementService agreementService;
    private final PerformanceService performanceService;
    private final ServiceService serviceService;
    private final VisitService visitService;
    private final TransferService transferService;
    private final JudicialConfirmService judicialService;
    private final PaymentPlanRepository planRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        User admin = user("admin", "admin123", "管理员", "王院长", "ADMIN", "诉调对接中心", null);
        CurrentUser.set(admin);
        try {
            seed();
        } finally {
            CurrentUser.clear();
        }
        log.info("===== 演示数据初始化完成 =====");
    }

    private void seed() {
        // ---- 用户账号 ----
        User window = user("window", "window123", "李窗口", "立案窗口人员", "WINDOW", "诉讼服务中心立案窗口", null);
        User m1 = user("mediator1", "mediator123", "张梅", "资深调解员", "MEDIATOR", "诉前调解中心", null);
        m1.setSpecialties("民间借贷,交通事故,小额买卖合同");
        userRepository.save(m1);
        User m2 = user("mediator2", "mediator123", "王建国", "调解员", "MEDIATOR", "诉前调解中心", null);
        m2.setSpecialties("物业费,邻里纠纷");
        userRepository.save(m2);
        User m3 = user("mediator3", "mediator123", "陈静", "线上调解员", "MEDIATOR", "诉前调解中心（线上组）", null);
        m3.setSpecialties("民间借贷,邻里纠纷");
        m3.setRemote(true);
        userRepository.save(m3);
        User ja = user("ja", "ja123", "周助理", "法官助理", "JUDGE_ASSISTANT", "立案庭", null);
        User exec = user("exec", "exec123", "吴联络", "执行联络员", "EXECUTION", "执行局联络组", null);

        User applicant1 = user("party1", "party123", "赵建国", "当事人（申请人）", "PARTY", null, "13800000001");
        User respondent1 = user("party3", "party123", "孙磊", "当事人（被申请人）", "PARTY", null, "13900000003");
        user("party2", "party123", "钱美华", "当事人", "PARTY", null, "13800000002");

        // ============ 案件1：民间借贷，分期协议，已履行+部分履行+逾期并存，履行中 ============
        CaseEntity c1 = buildCase(applicant1, "孙磊", "13900000003", "本市朝阳区XX路12号",
                "民间借贷", "赵建国诉孙磊民间借贷纠纷", new BigDecimal("50000"),
                "借条;银行转账凭证;微信催款记录", true, "URGENT", false);
        backdate(c1, 12);
        caseService.triage(c1.getId(), m1.getId(), "ONLINE", "事实清楚、标的不大，宜先行线上调解");
        scheduleService.create(c1.getId(), LocalDateTime.now().minusDays(10).withHour(10).withMinute(0),
                null, "ONLINE", "线上调解会议室 https://meet.court/room/1001", "首次排期", true);
        scheduleService.complete(latestSchedule(c1.getId()).getId(), "双方均到场");
        MediationRecord r1 = new MediationRecord();
        r1.setCaseId(c1.getId());
        r1.setSessionTime(LocalDateTime.now().minusDays(10).withHour(11));
        r1.setParticipants("申请人赵建国、被申请人孙磊、调解员张梅");
        r1.setFocusIssues("借款本金5万元及逾期利息是否认可；还款时间争议");
        r1.setConcessions("申请人放弃逾期利息800元；被申请人同意本金分期偿还，并由其堂兄孙强担保");
        r1.setPaymentPlan("分三期：9月10日还2万、9月30日还1.5万、10月20日还1.5万");
        r1.setGuarantor("孙强（被申请人堂兄，身份证已核验）");
        r1.setPerformanceDeadline(LocalDate.now().plusDays(24));
        r1.setApplyJudicialConfirm(true);
        r1.setResult("AGREED");
        mediationService.saveRecord(r1);

        Agreement a1 = draftAgreement(c1.getId(), new BigDecimal("50000"), "INSTALLMENTS",
                "孙强", LocalDate.now().plusDays(24), true,
                List.of(plan(LocalDate.now().minusDays(16), new BigDecimal("20000")),
                        plan(LocalDate.now().plusDays(4), new BigDecimal("15000")),
                        plan(LocalDate.now().minusDays(2), new BigDecimal("15000"))));
        agreementService.sign(a1.getId(), "APPLICANT", "赵建国", true, 0.97, null, "127.0.0.1");
        agreementService.sign(a1.getId(), "RESPONDENT", "孙磊", true, 0.95, null, "127.0.0.1");
        // 第一期已付清，第二期部分履行，第三期逾期（末尾统一扫描）
        List<PaymentPlan> p1 = planRepository.findByAgreementIdOrderByPeriodNoAsc(a1.getId());
        performanceService.confirmPayment(p1.get(0).getId(), new BigDecimal("20000"), "银行转账到账");
        performanceService.confirmPayment(p1.get(1).getId(), new BigDecimal("8000"), "仅筹到8000元，余款承诺月底补足");
        serviceService.serve(c1.getId(), "调解协议书", "孙磊", "13900000003", "SMS",
                "您与赵建国的调解协议已签署生效，请按约履行。");
        VisitRecord v1 = new VisitRecord();
        v1.setCaseId(c1.getId());
        v1.setAgreementId(a1.getId());
        v1.setChannel("PHONE");
        v1.setTarget("被申请人");
        v1.setResult("PARTIAL");
        v1.setContent("被申请人称工程款未结算，第二期仅能部分履行，请求宽限；已告知逾期后果并记录在案。");
        v1.setVisitorName("吴联络");
        visitService.save(v1);
        JudicialConfirm j1 = judicialService.generate(a1.getId());
        judicialService.submit(j1.getId());

        // ============ 案件2：物业费，多次改期+缺料+失联，拟转立案 ============
        CaseEntity c2 = buildCase(null, "李大华", "13700000007", "本市海淀区XX小区3-201",
                "物业费", "和谐物业公司诉李大华物业服务费纠纷", new BigDecimal("3200"),
                "物业服务合同;欠费明细（业主签收记录待补）", false, "NORMAL", false);
        backdate(c2, 9);
        caseService.triage(c2.getId(), m2.getId(), "OFFLINE", "业主坚持现场调解");
        scheduleService.create(c2.getId(), LocalDateTime.now().minusDays(8).withHour(14),
                null, "OFFLINE", "本院第三调解室", "首次排期", false);
        scheduleService.create(c2.getId(), LocalDateTime.now().minusDays(5).withHour(14),
                null, "OFFLINE", "本院第三调解室", "被申请人体检改期", false);
        scheduleService.create(c2.getId(), LocalDateTime.now().plusDays(2).withHour(14),
                null, "OFFLINE", "本院第三调解室", "申请人时间冲突，二次改期", false);
        mediationService.markEvidenceMissing(c2.getId(), "缺少业主签收的催缴通知回执及近两年物业费标准公示材料");
        mediationService.markUnreachable(c2.getId(), "9月21日、22日、23日连续三天电话关机、短信未回，上门无人应答");
        serviceService.serve(c2.getId(), "调解通知书", "李大华", "13700000007", "SMS",
                "请于规定时间参加调解，无正当理由缺席将依法转入诉讼程序。");

        // ============ 案件3：交通事故，调解中 ============
        CaseEntity c3 = buildCase(null, "刘强", "13600000006", "外地：河北省廊坊市XX街8号",
                "交通事故", "王芳诉刘强机动车交通事故责任纠纷", new BigDecimal("120000"),
                "事故认定书;医疗费用票据;车辆定损单;误工证明", true, "VERY_URGENT", true);
        backdate(c3, 4);
        caseService.triage(c3.getId(), m1.getId(), "ONLINE", "被申请人在外地，且伤情鉴定时效紧迫，安排擅长交通事故的调解员线上处理");
        scheduleService.create(c3.getId(), LocalDateTime.now().plusDays(1).withHour(9).withMinute(30),
                null, "ONLINE", "线上调解会议室 https://meet.court/room/1003", "特急优先排期", true);
        scheduleService.complete(latestSchedule(c3.getId()).getId(), "先行核对证据");
        MediationRecord r3 = new MediationRecord();
        r3.setCaseId(c3.getId());
        r3.setSessionTime(LocalDateTime.now().withHour(10));
        r3.setParticipants("王芳代理人、刘强、调解员张梅、保险公司线上列席");
        r3.setFocusIssues("医疗费中自费项目分担；误工费计算标准；保险公司理赔范围");
        r3.setConcessions("申请人同意误工费按月均工资八折主张；被申请人愿配合保险理赔");
        r3.setResult("PENDING");
        r3.setRemark("保险公司需补充理赔核算单，下周再次调解");
        mediationService.saveRecord(r3);

        // ============ 案件4：邻里纠纷，待分流（紧急） ============
        CaseEntity c4 = buildCase(null, "吴刚", "13500000005", "本市西城区XX胡同18号",
                "邻里纠纷", "周明诉吴刚相邻关系（漏水/噪音）纠纷", BigDecimal.ZERO,
                "房屋漏水照片;社区调解记录;录音", true, "URGENT", false);
        c4.setDescription("楼上卫生间漏水导致天花板受损，双方发生口角，申请人请求尽快介入防止矛盾激化。");
        caseRepository.save(c4);

        // ============ 案件5：小额买卖合同，一次性履行完毕，已结案 ============
        CaseEntity c5 = buildCase(null, "宏达装修有限公司", "010-88886666", "本市丰台区XX建材市场B区",
                "小额买卖合同", "诚信建材经营部诉宏达装修货款纠纷", new BigDecimal("28000"),
                "供货单;对账单;欠款确认微信记录", true, "NORMAL", false);
        backdate(c5, 22);
        caseService.triage(c5.getId(), m1.getId(), "ONLINE", "双方均有调解意愿");
        scheduleService.create(c5.getId(), LocalDateTime.now().minusDays(20).withHour(15),
                null, "ONLINE", "线上调解会议室 https://meet.court/room/1005", null, true);
        scheduleService.complete(latestSchedule(c5.getId()).getId(), null);
        MediationRecord r5 = new MediationRecord();
        r5.setCaseId(c5.getId());
        r5.setSessionTime(LocalDateTime.now().minusDays(20).withHour(16));
        r5.setFocusIssues("货款28000元给付时间");
        r5.setConcessions("申请人同意减免违约金1200元；被申请人承诺十日内一次性付清");
        r5.setPerformanceDeadline(LocalDate.now().minusDays(8));
        r5.setResult("AGREED");
        mediationService.saveRecord(r5);
        Agreement a5 = draftAgreement(c5.getId(), new BigDecimal("28000"), "LUMP_SUM",
                null, LocalDate.now().minusDays(8), false,
                List.of(plan(LocalDate.now().minusDays(10), new BigDecimal("28000"))));
        agreementService.sign(a5.getId(), "APPLICANT", "诚信建材经营部（法定代表人）", true, 0.96, null, "127.0.0.1");
        agreementService.sign(a5.getId(), "RESPONDENT", "宏达装修有限公司（法定代表人）", true, 0.98, null, "127.0.0.1");
        PaymentPlan p5 = planRepository.findByAgreementIdOrderByPeriodNoAsc(a5.getId()).get(0);
        performanceService.confirmPayment(p5.getId(), new BigDecimal("28000"), "对公转账已到账");
        VisitRecord v5 = new VisitRecord();
        v5.setCaseId(c5.getId());
        v5.setAgreementId(a5.getId());
        v5.setChannel("PHONE");
        v5.setTarget("双方");
        v5.setResult("PERFORMED");
        v5.setContent("双方确认货款两清，无其他争议，对调解结果满意。");
        v5.setVisitorName("吴联络");
        visitService.save(v5);

        // ============ 案件6：民间借贷，被申请人失联，送达后推进中 ============
        CaseEntity c6 = buildCase(null, "马涛", "13400000004", "本市通州区XX村（户籍地）",
                "民间借贷", "林晓燕诉马涛民间借贷纠纷", new BigDecimal("8000"),
                "借条;支付宝转账记录", false, "NORMAL", false);
        backdate(c6, 15);
        caseService.triage(c6.getId(), m3.getId(), "ONLINE", "先尝试线上联系，失联则电子送达");
        mediationService.markUnreachable(c6.getId(), "电话停机、支付宝留言未读；社区反馈其长期不在户籍地居住");
        serviceService.serve(c6.getId(), "调解通知书", "马涛", "13400000004", "ONLINE",
                "诉前调解中心已受理林晓燕诉你民间借贷纠纷，请于收到本通知之日起三日内联系本院。");

        // ============ 案件7：民间借贷，协议逾期未履行，已转立案 ============
        CaseEntity c7 = buildCase(null, "郑浩", "13300000003", "本市石景山区XX楼502",
                "民间借贷", "冯丽诉郑浩民间借贷纠纷", new BigDecimal("36000"),
                "借条;转账凭证;催收通话录音", true, "NORMAL", false);
        backdate(c7, 33);
        caseService.triage(c7.getId(), m3.getId(), "ONLINE", null);
        scheduleService.create(c7.getId(), LocalDateTime.now().minusDays(30).withHour(10),
                null, "ONLINE", "线上调解会议室 https://meet.court/room/1007", null, true);
        scheduleService.complete(latestSchedule(c7.getId()).getId(), null);
        MediationRecord r7 = new MediationRecord();
        r7.setCaseId(c7.getId());
        r7.setSessionTime(LocalDateTime.now().minusDays(30).withHour(11));
        r7.setFocusIssues("36000元借款返还期限");
        r7.setConcessions("被申请人承诺两期付清，申请人同意不主张利息");
        r7.setResult("AGREED");
        mediationService.saveRecord(r7);
        Agreement a7 = draftAgreement(c7.getId(), new BigDecimal("36000"), "INSTALLMENTS",
                null, LocalDate.now().minusDays(5), false,
                List.of(plan(LocalDate.now().minusDays(20), new BigDecimal("18000")),
                        plan(LocalDate.now().minusDays(5), new BigDecimal("18000"))));
        agreementService.sign(a7.getId(), "APPLICANT", "冯丽", true, 0.95, null, "127.0.0.1");
        agreementService.sign(a7.getId(), "RESPONDENT", "郑浩", true, 0.94, null, "127.0.0.1");
        performanceService.remind(planRepository.findByAgreementIdOrderByPeriodNoAsc(a7.getId()).get(0).getId(),
                "首期款到期未付，电话催告");
        serviceService.serve(c7.getId(), "履行催告书", "郑浩", "13300000003", "SMS",
                "您未按调解协议履行，逾期将依法转入诉讼程序。");
        TransferRequest t7 = transferService.apply(c7.getId(), "冯丽（申请人）", "OVERDUE",
                "两期款项均已到期，经催告仍分文未付，申请转立案诉讼解决。");
        transferService.review(t7.getId(), true, "（2026）京0105民初8866号",
                "协议逾期未履行，连续过程材料齐全，同意接收立案。");

        // ============ 案件8：邻里纠纷，调解不成 ============
        CaseEntity c8 = buildCase(null, "黄强", "13200000002", "本市东城区XX大院9号",
                "邻里纠纷", "孙建华诉黄强排除妨害纠纷", new BigDecimal("2000"),
                "现场照片;出警记录", false, "NORMAL", false);
        backdate(c8, 7);
        caseService.triage(c8.getId(), m2.getId(), "OFFLINE", null);
        scheduleService.create(c8.getId(), LocalDateTime.now().minusDays(6).withHour(9),
                null, "OFFLINE", "本院第三调解室", null, true);
        scheduleService.complete(latestSchedule(c8.getId()).getId(), null);
        MediationRecord r8 = new MediationRecord();
        r8.setCaseId(c8.getId());
        r8.setSessionTime(LocalDateTime.now().minusDays(6).withHour(10));
        r8.setFocusIssues("公共通道堆放杂物是否应予清除");
        r8.setConcessions("申请人同意给予三日清理期；被申请人拒绝清理，双方分歧较大");
        r8.setResult("NO_AGREEMENT");
        mediationService.saveRecord(r8);
        caseService.updateStatus(c8.getId(), "FAILED", "经调解双方无法达成一致，调解员建议依法诉讼");

        // ---- 统一扫描逾期节点（模拟每日08:00定时任务） ----
        int overdue = performanceService.scanOverdueManual();
        log.info("初始化逾期扫描：标记 {} 个逾期履行节点", overdue);
    }

    // ---------------- helpers ----------------

    private User user(String username, String pwd, String realName, String title, String role, String dept, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setRealName(realName);
        u.setTitle(title);
        u.setRole(role);
        u.setPhone(phone);
        u.setEnabled(true);
        u.setRemote(false);
        return userRepository.save(u);
    }

    private CaseEntity buildCase(User applicantUser, String respondent, String respPhone, String respAddr,
                            String cause, String title, BigDecimal amount, String evidence,
                            boolean willingOnline, String urgency, boolean remoteParty) {
        CaseEntity c = new CaseEntity();
        c.setTitle(title);
        c.setCauseType(cause);
        c.setClaimAmount(amount);
        c.setApplicantName(applicantUser == null ? title.split("诉")[0] : applicantUser.getRealName());
        c.setApplicantPhone(applicantUser == null ? "13800000000" : applicantUser.getPhone());
        c.setApplicantUserId(applicantUser == null ? null : applicantUser.getId());
        c.setRespondentName(respondent);
        c.setRespondentPhone(respPhone);
        c.setRespondentAddress(respAddr);
        c.setEvidenceSummary(evidence);
        c.setWillingOnline(willingOnline);
        c.setUrgency(urgency);
        c.setRemoteParty(remoteParty);
        return caseService.create(c);
    }

    private Agreement draftAgreement(Long caseId, BigDecimal amount, String method, String guarantor,
                                     LocalDate deadline, boolean judicial, List<PaymentPlan> plans) {
        Agreement a = new Agreement();
        a.setCaseId(caseId);
        a.setTotalAmount(amount);
        a.setPaymentMethod(method);
        a.setGuarantor(guarantor);
        a.setDeadline(deadline);
        a.setApplyJudicialConfirm(judicial);
        return agreementService.draft(a, plans);
    }

    private PaymentPlan plan(LocalDate due, BigDecimal amount) {
        PaymentPlan p = new PaymentPlan();
        p.setDueDate(due);
        p.setAmount(amount);
        return p;
    }

    private void backdate(CaseEntity c, int daysAgo) {
        c.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        caseRepository.save(c);
    }

    private Schedule latestSchedule(Long caseId) {
        return scheduleRepository.findByCaseIdOrderByVersionDesc(caseId).stream()
                .filter(s -> !"CANCELLED".equals(s.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("案件 " + caseId + " 尚无有效排期"));
    }
}
