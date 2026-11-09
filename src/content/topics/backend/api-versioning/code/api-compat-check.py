def breaking_changes(before, after, direction):
    """Lists what would break existing clients. Run it in CI against the published contract.

    `before` and `after` map a field name to {"type": ..., "required": ...}.
    """
    problems = []
    for name, old in before.items():
        now = after.get(name)
        if now is None:
            problems.append(f"removed: {name}")
        elif now["type"] != old["type"]:
            problems.append(f"type changed: {name}")
        elif direction == "request" and now["required"] and not old["required"]:
            problems.append(f"now required: {name}")
        elif direction == "response" and old["required"] and not now["required"]:
            problems.append(f"no longer guaranteed: {name}")
    if direction == "request":
        for name, spec in after.items():
            if spec["required"] and name not in before:
                problems.append(f"new required field: {name}")
    return problems
