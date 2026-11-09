def assemble(instructions, tools, history, retrieved, question, today):
    """Renders a prompt with the most stable parts first.

    Successive requests then share the longest possible prefix. Anything that changes per
    request goes last.
    """
    # Sorted: the same tools in a different order would be a different prefix.
    ordered = sorted(tools, key=lambda t: t["name"])
    tool_block = "\n".join(f"{t['name']}: {t['schema']}" for t in ordered)
    return [
        {"role": "system", "text": f"{instructions}\n\nTools:\n{tool_block}"},
        *history,
        # Today's date lives here, at the end. In the system prompt it would break the cache daily.
        {"role": "user", "text": f"Date: {today}\n\n{retrieved}\n\n{question}"},
    ]


def shared_prefix(a, b):
    """For a test: how many leading messages two rendered prompts share."""
    n = 0
    while n < len(a) and n < len(b) and a[n] == b[n]:
        n += 1
    return n
