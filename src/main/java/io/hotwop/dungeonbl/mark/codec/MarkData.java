package io.hotwop.dungeonbl.mark.codec;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MarkData{
    boolean optional() default false;
    boolean saveOnly() default false;
    int order() default 0;
    @NotNull String argument() default "";
    @NotNull String lore() default "";
}
