package houtbecke.rs.le.interceptor;

public class BaseIntercepting {

    public final int id;
    final LeInterceptor leInterceptor;

    public BaseIntercepting(LeInterceptor leInterceptor) {
        this.leInterceptor = leInterceptor;
        id = ++leInterceptor.counter;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName().replace("Intercepting", "")+
                '[' + id + "] ";
    }
}