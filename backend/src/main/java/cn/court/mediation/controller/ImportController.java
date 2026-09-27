package cn.court.mediation.controller;

import cn.court.mediation.common.ApiResponse;
import cn.court.mediation.security.CurrentUser;
import cn.court.mediation.service.ImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    /** 批量导入（CSV 文本，或前端文件读取后传入） */
    @PostMapping("/cases")
    public ApiResponse<Map<String, Object>> importCases(@RequestBody Map<String, String> body,
                                                        HttpServletRequest request) {
        String csv = body.get("csv");
        if (csv == null || csv.isBlank()) {
            return ApiResponse.fail("CSV 内容为空");
        }
        return ApiResponse.ok(importService.importCsv(csv, CurrentUser.get(request)));
    }

    /** 导入模板 */
    @GetMapping("/template")
    public ApiResponse<String> template() {
        return ApiResponse.ok("""
                案由,申请人,申请人电话,被申请人,被申请人电话,被申请人地址,是否外地,诉求金额,诉求摘要,证据清单,愿意线上调解,紧急程度
                民间借贷,张三,13800000001,李四,13900000002,本市某街道,否,30000,借款3万元到期未还,借条;转账凭证,是,普通
                物业费,阳光花园物业,0571-88888888,王五,13700000003,阳光花园3-201,否,3600,拖欠两年物业费,物业服务合同;催缴记录,是,普通
                """);
    }
}
