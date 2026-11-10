def plan(desired: dict[str, dict], actual: dict[str, dict]) -> list[tuple[str, str]]:
    """The heart of every declarative tool (Terraform plan, Kubernetes controllers): compare
    what the code declares with what exists, and list only the differences. Running it again
    on an already-matching world produces nothing to do."""
    actions = []
    for name in sorted(desired):
        if name not in actual:
            actions.append(("create", name))
        elif actual[name] != desired[name]:
            actions.append(("update", name))
    for name in sorted(actual):
        # exists but no longer declared, or created by hand and not in code: drift
        if name not in desired:
            actions.append(("delete", name))
    return actions
