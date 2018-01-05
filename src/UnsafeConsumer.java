import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface UnsafeConsumer<T, E extends Exception> {
	public void accept(T x) throws E;

	@SuppressWarnings("unchecked")
	public default void acceptOrElse(T x, BiConsumer<? super T, ? super E> handler) {
		try {
			accept(x);
		}
		catch(Exception e) {
			handler.accept(x, (E)e);
		}
	}

	public default Consumer<? super T> suppress() {
		return x -> {
			acceptOrElse(x, (y, e) -> { throw new RuntimeException(e); });
		};
	}
}
