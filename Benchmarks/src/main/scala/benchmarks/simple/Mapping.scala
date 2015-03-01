package benchmarks.simple

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import benchmarks.{RIParam, Util}
import interface.ReactiveInterface
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole


abstract class SomeState {

  var riparam: RIParam = _

  lazy val RI: ReactiveInterface = riparam.RI

  import RI.SignalOps

  var input: AtomicInteger = new AtomicInteger(0)
  var source: RI.IVar[Int] = _
  var result: RI.ISignal[Int] = _

  @Setup(Level.Iteration)
  def setup(riparam: RIParam) = {
    this.riparam = riparam
    source = RI.makeVar(input.get())
    result = source.map(1.+).map(1.+).map(1.+)

  }
}

@State(Scope.Thread)
class LocalState extends SomeState

@State(Scope.Benchmark)
class SharedState extends SomeState


@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
class Mapping {

  @Benchmark
  def local(bh: Blackhole, state: LocalState) = {
    import state._
    RI.setVar(source)(input.incrementAndGet())
    RI.getSignal(result)
  }

  @Benchmark
  def shared(bh: Blackhole, state: SharedState) = {
    import state._
    RI.setVar(source)(input.incrementAndGet())
    RI.getSignal(result)
  }


}
