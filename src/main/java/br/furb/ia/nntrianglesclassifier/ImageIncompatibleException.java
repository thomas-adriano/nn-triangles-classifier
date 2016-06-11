package br.furb.ia.nntrianglesclassifier;

/**
 * Created by Thomas.Adriano on 10/06/2016.
 */
public class ImageIncompatibleException extends RuntimeException {

    public ImageIncompatibleException() {
    }

    public ImageIncompatibleException(String message) {
        super(message);
    }

    public ImageIncompatibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageIncompatibleException(Throwable cause) {
        super(cause);
    }

    public ImageIncompatibleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
