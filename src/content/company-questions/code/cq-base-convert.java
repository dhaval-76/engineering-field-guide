String convertBase(String number, int from, int to) {
    long value = 0;
    for (char c : number.toCharArray()) value = value * from + Character.digit(c, from);
    if (value == 0) return "0";
    StringBuilder out = new StringBuilder();
    for (; value > 0; value /= to) out.append(Character.forDigit((int) (value % to), to));
    return out.reverse().toString();
}
