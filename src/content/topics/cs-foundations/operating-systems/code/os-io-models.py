# asyncio: an event loop over non-blocking sockets (selectors → epoll on Linux)
import asyncio

async def handle(reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
    while data := await reader.read(4096):     # suspends this coroutine, not the thread
        writer.write(data)
        await writer.drain()
    writer.close()

async def main():
    server = await asyncio.start_server(handle, port=8080)
    async with server:
        await server.serve_forever()

asyncio.run(main())
