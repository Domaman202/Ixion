# Документация

* [Первая программа](#Первая-программа)
* [Переменные](#Переменные)
* [Функции](#Функции)
* [Списки](#Списки)
* [Типы](#Типы)
* [Условные конструкции](#Условные-конструкции)
    * [if / else](##if-else)
    * [case](##case)
* [Циклы](#Циклы)
* [Структуры](#Структуры)
* [Перечисления](#Перечисления)


# Первая программа
Первая программа: вывод в консоль hello world
В Ixion точкой входа служит функция `main`, именно там мы
и будем вызывать функцию для вывода текста в консоль. Для вывода
используется `print` или `println` из библиотеки `prelude`.
Стандарт prelude (о методах и их реализациях под разные бекенды)
описан в [документе]() (на данный момент документ приватный).

````scala
use <prelude>

def main() {
    println("Hello World")
}
````

# Переменные
Для создания переменной нужно воспользоваться 
ключевым словом `var`, далее идет имя переменной, оператор `=` и значение.

Пример 1:

````scala
use <prelude>

def main() {
    var h = "Hello "
    var w = "World"
    println(h + w)
}
````

Пример 2:

````scala
use <prelude>

def main() {
    var a = 10
    print(a)
}
````

# Функции

Сигнатура:

`def` имя (аргумент1: тип, аргумент2: тип): тип {..}

Пример функции, которая принимает два целых
числа и возвращает их сумму:

````scala
use <prelude>

def main() {
    println(sum(10, 2))
}

def sum(a : int, b : int): int {
    return a + b
}
````

# Списки

В Ixion не существует массивов, вместо этого используются списки - 
динамические массивы.

Список целых чисел и список строк:
````scala
def main(){
    var nums = [1,2,3,4,5]
    var names = ["Maxim", "Artyom", "Anton"]
}
````

Пустой список строк:
````scala
def main(){
    var my_list = string[]
}
````

Функция, которая возвращает список целых чисел:
````scala
use <prelude>

def main(){
    print(ret_list())
}

def ret_list() : int[] {
    return [1,2,3,4,5]
}
````

Получить элемент списка:
````scala
use <prelude>

def main(){
    var nums = [1,2,3]
    print(list_get(nums, 0))
}
````

# Типы

````scala

use <prelude>

type text = string

def main(){
    println(greeting("Artyom"))
}

def greeting(name : text) : text {
    return "Hello, " + name
}
````


# Условные конструкции

## if else

Пример 1:
````scala
use <prelude>

def main(){
    var flag = true
    if(flag) {
        print("yes :)")
    } else {
        print("no :(")
    }
}
````

Пример 2:
````scala
use <prelude>

def main(){
    var age = 18
    if(age >= 18) {
        print("hello!")
    } else if(age >= 16){
        print("go home")
    } else {
        print("go home kid")
    }
} 
````

## case

Пример pattern matching'a с алгебраическими типами

````scala
use <prelude>

type number = int | float

pub def main(){
    print_type(10)
    print_type(10.0f)
}

def print_type(num : number){
    case num {
        int i => println("value " + i + " is integer")
        float f => println("value " + f + " is float")
    }
}
````

# Циклы
