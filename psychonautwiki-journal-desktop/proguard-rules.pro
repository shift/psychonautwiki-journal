-keep public class com.isaakhanimann.journal.desktop.MainKt {
    public static void main(java.lang.String[]);
}

-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

-keep class kotlin.reflect.** { *; }
-keep class org.jetbrains.compose.** { *; }
-keep class androidx.compose.** { *; }