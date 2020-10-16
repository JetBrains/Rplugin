import numpy as np
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

import json

ENCODING = 'utf-8'


class RequestHandler(BaseHTTPRequestHandler):

    # keep-alive
    protocol_version = 'HTTP/1.1'

    def do_POST(self):
        print(self.headers) # Debug purposes
        try:
            content_length = int(self.headers.get('Content-Length'))
        except TypeError:
            # Content-Length header is missing
            self.send_response(400)
            return
        input_message = self.rfile.read(content_length)
        print("{} wrote:".format(self.client_address[0]))
        print(input_message.decode(ENCODING))
        completion_variants = ["1",
                               "something",
                               "good completion",
                               "I am in \u263a",
                               "ztrend     [60]",
                               "for(i in 1:10){",
                               "чек.тест",
                               "\"String\"",
                               "c(0, 1, 2:3, 700)"]
        scores = np.random.rand(len(completion_variants))
        answer = self.__construct_response_json(completion_variants, scores)
        print(answer)
        answer_as_bytes = answer.encode(ENCODING)
        self.send_response(200)
        self.__make_header(len(answer_as_bytes))
        self.wfile.write(answer_as_bytes)

    @staticmethod
    def __construct_response_json(variants, scores):
        completions = [{"text": v, "score": s} for v, s, in zip(variants, scores)]
        return json.dumps({"completionVariants": completions})

    def __make_header(self, content_length, content_type='application/json'):
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", content_length)
        self.end_headers()


host, port = sys.argv[1], int(sys.argv[2])
with HTTPServer((host, port), RequestHandler) as server:
    try:
        print("Serving")
        server.serve_forever()
    finally:
        server.shutdown()
