import json
import sys


def identity(x):
    return x


class PipeWrapper:
    def __init__(self, user_fn, map_input=identity, map_output=identity):
        self.user_fn = user_fn
        self.map_input = map_input
        self.map_output = map_output

    def run(self):
        for l in iter(sys.stdin.readline, ''):
            line = l.strip()
            input_data = json.loads(line)
            result = self.user_fn(input_data)
            result = self.map_output(result)
            output_data = json.dumps(result)
            print(output_data)
            sys.stdout.flush()
