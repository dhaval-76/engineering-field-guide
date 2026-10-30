def daily_temperatures(temps: list[int]) -> list[int]:
    answer = [0] * len(temps)
    waiting = []                                 # indices with decreasing temperatures
    for i, t in enumerate(temps):
        while waiting and t > temps[waiting[-1]]:
            j = waiting.pop()
            answer[j] = i - j
        waiting.append(i)
    return answer
