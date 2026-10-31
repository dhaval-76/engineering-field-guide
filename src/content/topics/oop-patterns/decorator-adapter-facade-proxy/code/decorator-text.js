// Each decorator wraps any object with render() and adds to its output.
const plain = (value) => ({ render: () => value });
const bold = (inner) => ({ render: () => `<b>${inner.render()}</b>` });
const italic = (inner) => ({ render: () => `<i>${inner.render()}</i>` });
const underline = (inner) => ({ render: () => `<u>${inner.render()}</u>` });

underline(italic(bold(plain('Hello')))).render(); // <u><i><b>Hello</b></i></u>
