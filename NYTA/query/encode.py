__author__ = 'Jiankai Dang'


def decode7bit(bytes):
    data = []
    bytes = list(bytes)
    value = 0
    shift = 0
    while len(bytes) != 0:
        byteval = ord(bytes.pop(0))
        if (byteval & 128) == 0:
            data.append((value << shift) | byteval)
            shift = 0
            value = 0
            continue
        value = (value << shift) | (byteval & 0x7F)
        shift = 7
    return data