package benchmarks

import java.util.concurrent.TimeUnit

import benchmarks.PhilosopherTable.{Seating, Thinking}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole, ThreadParams}
import rescala.turns.Engines

import scala.annotation.tailrec
import scala.util.Random

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
class PhilosopherCompetition {

  @Benchmark
  def eat(comp: Competition, params: ThreadParams): Unit = {
    val myBlock = comp.blocks(params.getThreadIndex % comp.blocks.length)
    val seating = myBlock(Random.nextInt(myBlock.length))
    comp.table.eatOnce(seating)
    seating.philosopher.set(Thinking)(comp.table.engine)
  }

  //@Benchmark
  def emptySync(comp: Competition): Unit = comp.synchronized(Blackhole.consumeCPU(0))
}


@State(Scope.Benchmark)
class Competition {

  @Param(Array("pessimistic", "synchron"))
  var engineName: String = _

  @Param(Array("32", "64", "128", "256"))
  var philosophers: Int = _

  var table: PhilosopherTable = _

  var blocks: Array[Array[Seating]] = _

  @Setup
  def setup(params: BenchmarkParams) = {
    table = new PhilosopherTable(philosophers)(Engines.byName(engineName))
    blocks = deal(table.seatings.toList, List.fill(params.getThreads)(Nil)).map(_.toArray).toArray
  }

  @TearDown(Level.Iteration)
  def cleanEating(): Unit = {
    print(s"actually eaten: ${table.eaten.get()} measured: ")
    table.eaten.set(0)
    table.seatings.foreach(_.philosopher.set(Thinking)(table.engine))
  }

  @tailrec
  final def deal[A](deck: List[A], hands: List[List[A]]): List[List[A]] = deck match {
    case Nil => hands
    case card :: rest => deal(rest, hands.tail :+ (card :: hands.head))
  }

}