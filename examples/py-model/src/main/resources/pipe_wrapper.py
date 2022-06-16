import sys
from enum import Enum


def identity(x):
    return x


class PipeState(Enum):
    WAITING_BATCH = 1
    BATCH_STARTED = 2


class PipeWrapper:
    buffer = []
    state = PipeState.WAITING_BATCH

    def __init__(self, user_fn, map_input=identity, map_output=identity):
        self.user_fn = user_fn
        self.map_input = map_input
        self.map_output = map_output

    def run(self):
        for l in iter(sys.stdin.readline, ''):
            line = l.strip()
            if line == '__ndk_bs' and self.state == PipeState.WAITING_BATCH:
                self.buffer = []
                self.state = PipeState.BATCH_STARTED
            elif line == '__ndk_be' and self.state == PipeState.BATCH_STARTED:
                result = self.user_fn(self.buffer)
                result = self.map_output(result)
                self.state = PipeState.WAITING_BATCH
                print('__ndk_bs')
                print(result)
                print('__ndk_be')
                sys.stdout.flush()
            elif self.state == PipeState.BATCH_STARTED:
                self.buffer.append(self.map_input(line))
            else:
                raise RuntimeError(f'Unexpected input in state: {self.state}, input: {line}')
