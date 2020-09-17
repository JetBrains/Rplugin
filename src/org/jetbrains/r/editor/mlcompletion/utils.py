from socketserver import BaseServer, BaseRequestHandler
from typing import Optional
from contextlib import contextmanager
import errno


REGISTERED_PORTS_START = 1_024
REGISTERED_PORTS_END = 49_151


@contextmanager
def bind_to_free_port(
    server_class: BaseServer,
    host: str,
    start_port: Optional[int],
    handler: BaseRequestHandler
):
    start_port = start_port or REGISTERED_PORTS_START
    for port_number in range(start_port, REGISTERED_PORTS_END + 1):
        try:
            with server_class((host, port_number), handler) as server:
                # Following try clause describes functional contextmanager behaviour
                try:
                    yield server
                finally:
                    server.shutdown()
                    return
        except OSError as e:
            if e.errno == errno.EADDRINUSE:
                # Address in use error, continue searching for a free port
                continue
            raise e
