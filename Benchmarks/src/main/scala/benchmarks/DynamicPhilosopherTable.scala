package benchmarks

import benchmarks.PhilosopherTable._
import rescala.turns.{Engine, Turn}
import rescala.{Signals, Var}

class DynamicPhilosopherTable(philosopherCount: Int, work: Long)(implicit engine: Engine[Turn]) extends PhilosopherTable(philosopherCount, work)(engine) {


  override def createTable(tableSize: Int): Seq[Seating] = {
    def mod(n: Int): Int = (n + tableSize) % tableSize

    val phils = for (i <- 0 until tableSize) yield Var[Philosopher](Thinking)

    val forks = for (i <- 0 until tableSize) yield {
      val nextCircularIndex = mod(i + 1)
      Signals.dynamic(phils(i), phils(nextCircularIndex)) { turn =>
        phils(i)(turn) match {
          case Hungry => Taken(i.toString)
          case Thinking =>
            phils(nextCircularIndex)(turn) match {
              case Hungry => Taken(nextCircularIndex.toString)
              case Thinking => Free
          }
        }
      }

    }

    for (i <- 0 until tableSize) yield {
      val ownName = i.toString
      val vision = Signals.dynamic(forks(i), forks(mod(i - 1))) { turn =>
        forks(i)(turn) match {
          case Taken(name) if name != ownName => WaitingFor(name)
          case Taken(`ownName`) => Eating
          case Free => forks(mod(i - 1))(turn) match {
            case Free => Ready
            case Taken(name) => WaitingFor(name)
          }
        }
      }
      Seating(i, phils(i), forks(i), forks(mod(i - 1)), vision)
    }
  }

}
