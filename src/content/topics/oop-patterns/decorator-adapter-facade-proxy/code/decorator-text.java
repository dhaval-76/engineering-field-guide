interface Text { String render(); }
record Plain(String value) implements Text { public String render() { return value; } }
record Bold(Text inner) implements Text {
    public String render() { return "<b>" + inner.render() + "</b>"; }
}
record Italic(Text inner) implements Text {
    public String render() { return "<i>" + inner.render() + "</i>"; }
}
record Underline(Text inner) implements Text {
    public String render() { return "<u>" + inner.render() + "</u>"; }
}

// <u><i><b>Hello</b></i></u>
new Underline(new Italic(new Bold(new Plain("Hello")))).render();
