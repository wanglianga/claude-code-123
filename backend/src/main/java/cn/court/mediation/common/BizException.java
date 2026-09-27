package cn.court.mediation.common;

/** 业务异常：返回可读提示，不打印错误堆栈 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
