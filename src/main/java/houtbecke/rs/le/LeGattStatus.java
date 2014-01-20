package houtbecke.rs.le;

public enum LeGattStatus {
    SUCCESS,
    READ_NOT_PERMITTED,
    WRITE_NOT_PERMITTED,
    INSUFFICIENT_AUTHENTICATION,
    REQUEST_NOT_SUPPORTED,
    INSUFFICIENT_ENCRYPTION,
    INVALID_OFFSET,
    INVALID_ATTRIBUTE_LENGTH,
    FAILURE
}