"""Tiny JSON-schema subset validator (type, required, enum, min/max, minLength/maxLength, items, additionalProperties)."""

_TYPES = {"string": str, "boolean": bool, "object": dict, "array": list}


def _type_ok(v, t):
    if t == "integer":
        return isinstance(v, int) and not isinstance(v, bool)
    if t == "number":
        return isinstance(v, (int, float)) and not isinstance(v, bool)
    if t == "null":
        return v is None
    return isinstance(v, _TYPES[t])


def validate(value, schema, path="$"):
    """Return a list of human-readable errors; empty means valid. Messages are fed back to the model on retry."""
    errs = []
    t = schema.get("type")
    if t is not None:
        ts = t if isinstance(t, list) else [t]
        if not any(_type_ok(value, x) for x in ts):
            return ["%s: expected %s, got %s" % (path, "/".join(ts), type(value).__name__)]
    if "enum" in schema and value not in schema["enum"]:
        errs.append("%s: must be one of %s" % (path, schema["enum"]))
    if isinstance(value, str):
        if len(value) < schema.get("minLength", 0):
            errs.append("%s: too short (min %d characters)" % (path, schema["minLength"]))
        if len(value) > schema.get("maxLength", 10 ** 9):
            errs.append("%s: too long (max %d characters)" % (path, schema["maxLength"]))
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        if "minimum" in schema and value < schema["minimum"]:
            errs.append("%s: must be >= %s" % (path, schema["minimum"]))
        if "maximum" in schema and value > schema["maximum"]:
            errs.append("%s: must be <= %s" % (path, schema["maximum"]))
    if isinstance(value, dict):
        props = schema.get("properties", {})
        for k in schema.get("required", []):
            if k not in value:
                errs.append("%s: missing required field '%s'" % (path, k))
        for k, v in value.items():
            if k in props:
                errs += validate(v, props[k], "%s.%s" % (path, k))
            elif schema.get("additionalProperties") is False:
                errs.append("%s: unexpected field '%s'" % (path, k))
    if isinstance(value, list):
        if len(value) < schema.get("minItems", 0):
            errs.append("%s: needs at least %d items" % (path, schema["minItems"]))
        if len(value) > schema.get("maxItems", 10 ** 9):
            errs.append("%s: needs at most %d items" % (path, schema["maxItems"]))
        if "items" in schema:
            for i, v in enumerate(value):
                errs += validate(v, schema["items"], "%s[%d]" % (path, i))
    return errs
