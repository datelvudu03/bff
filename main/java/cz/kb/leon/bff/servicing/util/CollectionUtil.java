package cz.kb.leon.bff.servicing.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CollectionUtil {

    private CollectionUtil() {
    }

    public static boolean isNull(Collection<?> collection) {
        return collection == null;
    }

    public static boolean isNotNull(Collection<?> collection) {
        return !isNull(collection);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return isNull(collection) || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static <T> boolean containsValue(Collection<T> collection, T value) {
        return isNotEmpty(collection) && collection.contains(value);
    }

    public static <T> boolean containsAllValues(Collection<T> collection, Collection<T> values) {
        return isNotEmpty(collection) && collection.containsAll(values);
    }

    public static <T> List<T> filterByConditionAsList(Collection<T> collection, Predicate<T> filterPredicate) {
        if (isEmpty(collection)) {
            return List.of();
        }
        return collection.stream().filter(Objects::nonNull).filter(filterPredicate).toList();
    }

    public static <T> T filterByConditionAndReturnFirstOrThrow(Collection<T> collection, Predicate<T> filterPredicate, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (isEmpty(collection)) {
            throw exceptionSupplier.get();
        }
        return collection.stream().filter(Objects::nonNull).filter(filterPredicate).findFirst().orElseThrow(exceptionSupplier);
    }

    public static <T> T filterByConditionAndReturnFirst(Collection<T> collection, Predicate<T> filterPredicate) {
        return safelyReturnNthElementValue(filterByConditionAsList(collection, filterPredicate), 0);
    }

    public static <T> void filterByConditionAndApply(Collection<T> collection, Predicate<T> filterPredicate, Consumer<T> command) {
        if (isEmpty(collection)) {
            return;
        }
        collection.stream().filter(filterPredicate).forEach(command);
    }

    public static <T, V> V safelyReturnNthElementValue(List<T> collection, int ord, Function<T, V> func) {
        var element = safelyReturnNthElementValue(collection, ord);
        if (element == null) {
            return null;
        }
        return func.apply(element);
    }

    public static <T> T safelyReturnNthElementValue(List<T> collection, int ord) {
        if (isEmpty(collection) || ord < 0 || ord >= collection.size()) {
            return null;
        }
        return collection.get(ord);
    }

    public static <T> T checkAndReturnFirst(Collection<T> collection, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (isNull(collection)) {
            throw exceptionSupplier.get();
        }
        return collection.stream().findFirst().orElseThrow(exceptionSupplier);
    }

    public static <T> void addToCollectionSafely(T value, Supplier<Collection<T>> collectionMethod, Consumer<T> creationMethod) {
        var collection = collectionMethod.get();
        if (isNotNull(collection)) {
            if (!containsValue(collection, value)) {
                collection.add(value);
            }
        } else {
            creationMethod.accept(value);
        }
    }

    public static List<Long> toListOfLong(List<String> values) {
        return toListOfLong(values.stream());
    }

    public static List<Long> toListOfLong(Stream<String> values) {
        return values.filter(Objects::nonNull).filter(v -> v.matches("\\-?\\d+")).map(Long::parseLong).toList();
    }

}
