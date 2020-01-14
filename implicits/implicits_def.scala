sealed trait SumMaker[A] {
  def zero: A
  def add(a: A, b: A): A
}

sealed trait MyList[A] {
  def sum(implicit maker: SumMaker[A]): A
}
case class MyNil[A]() extends MyList[A] {
  def sum(implicit maker: SumMaker[A]): A  = maker.zero
}
case class MyCons[A](head: A, tail: MyList[A]) extends MyList[A] {
  def sum(implicit maker: SumMaker[A]): A = maker.add(head, tail.sum)
}

object Examples {
  implicit def makerInt: SumMaker[Int] = {
    new SumMaker[Int] {
      def zero: Int = 0
      def add(a: Int, b: Int): Int = a + b
    }
  }

  implicit def makerDouble: SumMaker[Double] = {
    new SumMaker[Double] {
      def zero: Double = 0.0
      def add(a: Double, b: Double): Double = a + b
    }
  }

  def main(args: Array[String]) {
    println(MyCons(1, MyCons(2, MyCons(3, MyNil[Int]()))).sum)
    println(MyCons(1.1, MyCons(2.2, MyCons(3.3, MyNil[Double]()))).sum)
  }
}
