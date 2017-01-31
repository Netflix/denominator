package denominator.ultradns.model;

import java.util.TreeMap;

public class UltraError {

    private int errorCode;
    private String errorMessage;
    private TreeMap<String, Object> errorData;

    public UltraError(int errorCode, String errorMessage)
    {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public final int getErrorCode()
    {
        return this.errorCode;
    }

    public final void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }

    public final String getErrorMessage()
    {
        return this.errorMessage;
    }

    public final void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public TreeMap<String, Object> getErrorData() {
        return this.errorData;
    }

    public void setErrorData(TreeMap<String, Object> errorData) {
        this.errorData = errorData;
    }
}
