package dev.parez.sidekick.preferences

public sealed class PreferenceDefinition<T : Any>(
    public val key: String,
    public val label: String,
    public val description: String,
    public val defaultValue: T,
)

public class BooleanPref(key: String, label: String, description: String, defaultValue: Boolean) :
    PreferenceDefinition<Boolean>(key, label, description, defaultValue)

public class StringPref(key: String, label: String, description: String, defaultValue: String) :
    PreferenceDefinition<String>(key, label, description, defaultValue)

public class IntPref(key: String, label: String, description: String, defaultValue: Int) :
    PreferenceDefinition<Int>(key, label, description, defaultValue)

public class LongPref(key: String, label: String, description: String, defaultValue: Long) :
    PreferenceDefinition<Long>(key, label, description, defaultValue)

public class FloatPref(key: String, label: String, description: String, defaultValue: Float) :
    PreferenceDefinition<Float>(key, label, description, defaultValue)

public class DoublePref(key: String, label: String, description: String, defaultValue: Double) :
    PreferenceDefinition<Double>(key, label, description, defaultValue)

/**
 * A preference that can only take one of a fixed set of string values (enum entry names). The UI
 * renders each option as a selectable chip. The stored value and [defaultValue] are the enum entry
 * name strings (e.g. "FIRE").
 */
public class EnumPref(
    key: String,
    label: String,
    description: String,
    defaultValue: String,
    public val options: List<String>,
) : PreferenceDefinition<String>(key, label, description, defaultValue)
