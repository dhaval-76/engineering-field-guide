from dataclasses import dataclass, field


@dataclass(frozen=True)
class Selection:
    """One selected field. list_size is the page size for a list field, or 0 for a single value."""

    name: str
    list_size: int = 0
    children: list = field(default_factory=list)


def analyze(selections):
    """Returns (fields, depth, nested lists) if every list came back full."""
    fields = depth = nested_lists = 0
    for s in selections:
        below_fields, below_depth, below_lists = analyze(s.children)
        fields += 1 + max(1, s.list_size) * below_fields
        depth = max(depth, 1 + below_depth)
        nested_lists = max(nested_lists, (1 if s.list_size else 0) + below_lists)
    return fields, depth, nested_lists


def rejection_reason(query, max_fields, max_depth, max_nested_lists):
    """None means allowed. Runs before any resolver does."""
    fields, depth, nested_lists = analyze(query)
    if depth > max_depth:
        return f"too deep: {depth}"
    if nested_lists > max_nested_lists:
        return f"too many nested lists: {nested_lists}"
    if fields > max_fields:
        return f"too expensive: {fields} fields"
    return None
