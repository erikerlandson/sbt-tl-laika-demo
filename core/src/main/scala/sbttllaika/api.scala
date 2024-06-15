package sbttllaika

class Api(t: Int):
    def foo(x: Int): Int = x + t

    def goo(y: Int): Int = y + (2 * t)

object Api:
    def apply(t: Int): Api = new Api(t)
