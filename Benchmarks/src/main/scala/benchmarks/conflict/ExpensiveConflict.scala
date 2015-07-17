package benchmarks.conflict

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import benchmarks.{EngineParam, Workload}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, ThreadParams}
import rescala._
import rescala.graph.Spores
import rescala.turns.{Engine, Ticket, Turn}

@AuxCounters
@State(Scope.Thread)
class EvaluationCounter {
  var tried: Int = _
  var succeeded: Int = _

  @Setup(Level.Iteration)
  def reset() = {
    tried = 0
    succeeded = 0
  }

}

@State(Scope.Group)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(2)
class ExpensiveConflict[S <: Spores] {

  var input: AtomicInteger = new AtomicInteger(0)

  var cheapSource: Var[Int, S] = _
  var expensiveSource: Var[Int, S] = _
  var result: Signal[Int, S] = _
  var engine: Engine[S, Turn[S]] = _
  var tried: Int = _

  @Setup(Level.Iteration)
  def setup(engine: EngineParam, work: Workload) = {
    this.engine = engine.engine
    implicit val e = this.engine
    tried = 0
    cheapSource = Var(input.incrementAndGet())
    expensiveSource = Var(input.incrementAndGet())
    val expensive = expensiveSource.map{ v => tried += 1; work.consume(); v + 1}
    result = Signals.lift(expensive, cheapSource)(_ + _).map(_ + 1).map(_ + 1).map(_ + 1)
  }

  @Benchmark
  @Group("g")
  @GroupThreads(1)
  def cheap() = {
    cheapSource.set(input.incrementAndGet())(engine)
  }

  @Benchmark
  @Group("g")
  @GroupThreads(1)
  def expensive(counter: EvaluationCounter) = {
    expensiveSource.set(input.incrementAndGet())(engine)
    counter.tried += tried
    counter.succeeded += 1
    tried = 0
  }


}
