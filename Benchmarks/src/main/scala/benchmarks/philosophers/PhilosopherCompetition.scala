package benchmarks.philosophers

import java.util.concurrent.TimeUnit

import benchmarks.{Workload, EngineParam, Util}
import benchmarks.Util.deal
import benchmarks.philosophers.PhilosopherTable.{Seating, Thinking}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole, ThreadParams}
import rescala.graph.Spores
import rescala.turns.Engines

import scala.annotation.tailrec
import scala.util.Random

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
class PhilosopherCompetition[S <: Spores] {

  @Benchmark
  def eat(comp: Competition[S], params: ThreadParams, work: Workload): Unit = {
    val myBlock = comp.blocks(params.getThreadIndex % comp.blocks.length)
    while ( {
      val seating = myBlock(Random.nextInt(myBlock.length))
      val res = comp.table.tryEat(seating)
      if (res) seating.philosopher.set(Thinking)(comp.table.engine)
      !res
    }) {}

  }
}



@State(Scope.Benchmark)
class Competition[S <: Spores] {

  @Param(Array("16", "32", "64", "128", "256", "512", "1024", "2048"))
  var philosophers: Int = _

  @Param(Array("block", "alternating", "random", "third"))
  var layout: String = _

  @Param(Array("static", "dynamic"))
  var tableType: String = _

  var table: PhilosopherTable[S] = _

  var blocks: Array[Array[Seating[S]]] = _

  @Setup
  def setup(params: BenchmarkParams, work: Workload, engineParam: EngineParam) = {
    table = tableType match {
      case "static" => new PhilosopherTable(philosophers, work.work)(engineParam.engine)
      case "dynamic" => new DynamicPhilosopherTable(philosophers, work.work)(engineParam.engine)
    }
    blocks = (layout match {
      case "block" =>
        val perThread = table.seatings.size / params.getThreads
        table.seatings.sliding(perThread, perThread)
      case "alternating" => deal(table.seatings.toList,math.min(params.getThreads, philosophers))
      case "third" => deal(table.seatings.sliding(3, 3).map(_.head).toList, params.getThreads)
      case "random" => List(table.seatings)
    }).map(_.toArray).toArray
  }

  @TearDown(Level.Iteration)
  def cleanEating(): Unit = {
    //print(s"actually eaten: ${ table.eaten.get() } measured: ")
    table.eaten.set(0)
    table.seatings.foreach(_.philosopher.set(Thinking)(table.engine))
  }

}
