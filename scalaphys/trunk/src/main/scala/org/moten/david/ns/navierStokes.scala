/**
 * Provides a clear and concise solver for the
 * Navier Stokes equations over a rectangular
 * grid domain (regularly gridded) for an
 * incompressible liquid (sea water). Concise
 * and clear code is considered more important
 * than performance because concurrent/
 * distributed running of the routines will be
 *  used to provide performance scalability.
 */
package org.moten.david.ns

/**
 * Logs to System.out with a timestamp.
 */
object Logger {
  import java.util.Date
  import java.text.SimpleDateFormat
  val df = new SimpleDateFormat("HH:mm:ss.SSS")
  var infoEnabled = true
  var debugEnabled = false
  def info(msg: => AnyRef) = if (infoEnabled)
    println(df.format(new Date()) + " " + msg)
  def debug(msg: => AnyRef) = if (debugEnabled)
    println(df.format(new Date()) + " " + msg)
}
import Logger._

/**
 * Useful exceptions.
 */
object Throwing {
  def unexpected =
    throw new RuntimeException("program should not get to this point")

  def unexpected(message: String) =
    throw new RuntimeException(message)
}

import Throwing._

/**
 * X,Y horizontal coordinates (arbitrary coordinate system).
 * Z is height above sea level in m (all calculations
 * assume SI units).
 *
 */
object Direction extends Enumeration {
  type Direction = Value
  val X, Y, Z = Value
  def directions = List(X, Y, Z)
}
import Direction._

/**
 * The derivative type.
 */
object DerivativeType extends Enumeration {
  type Derivative = Value
  val FirstDerivative, SecondDerivative = Value
}
import DerivativeType._

/**
 * A mathematical vector in X,Y,Z space.
 *
 * @param x
 * @param y
 * @param z
 */
case class Vector(x: Double, y: Double, z: Double) {
  def this(list: List[Double]) {
    this(list(0), list(1), list(2))
  }
  def this(t: Tuple3[Double, Double, Double]) {
    this(t._1, t._2, t._3)
  }
  /**
   * Returns the value of the vector for the given direction.
   *
   * @param direction
   * @return
   */
  def get(direction: Direction): Double = {
    direction match {
      case X => x
      case Y => y
      case Z => z
    }
  }
  /**
   * Returns the dot product with another `Vector`.
   * @param v
   * @return
   */
  def *(v: Vector) = x * v.x + y * v.y + z * v.z

  /**
   * Returns the scalar product of this with a `Double` value.
   * @param d
   * @return
   */
  def *(d: Double) = Vector(x * d, y * d, z * d)

  /**
   * Returns difference of this with the given `Vector`.
   * @param v
   * @return
   */
  def minus(v: Vector) = Vector(x - v.x, y - v.y, z - v.z)

  /**
   * Returns difference of this with the given `Vector`.
   * @param v
   * @return
   */
  def -(v: Vector) = minus(v)

  /**
   * Returns the sum of this and the given `Vector`.
   * @param v
   * @return
   */
  def +(v: Vector) = add(v)
  def add(v: Vector) = Vector(x + v.x, y + v.y, z + v.z)
  def /(d: Double) = Vector(x / d, y / d, z / d)
  def /(v: Vector) = Vector(x / v.x, y / v.y, z / v.z)
  def sum = x + y + z
  def modify(direction: Direction, d: Double) = {
    Vector(if (direction equals X) d else x,
      if (direction equals Y) d else y,
      if (direction equals Z) d else z)
  }
  def ===(v: Vector) = this equals v
  def list = List(x, y, z)
}

/**
 * Companion object for Vector.
 */
object Vector {
  import Double._
  def zero = Vector(0, 0, 0)
}

import Vector._

/**
 * An ordering to help with readable String
 * representations of Vector collections.
 */
object VectorOrdering extends Ordering[Vector] {
  def compare(a: Vector, b: Vector): Int = {
    if (a.z != b.z) return a.z compare b.z
    else if (a.y != b.y) return a.y compare b.y
    else return a.x compare b.x
  }
}

/**
 * A 3 dimensional matrix.
 */
case class Matrix(row1: Vector, row2: Vector, row3: Vector) {
  def *(v: Vector) = Vector(row1 * v, row2 * v, row3 * v)
}

trait HasPosition {
  def position: Vector
}

trait HasValue {
  def value: Value
}

case class Boundary(position: Vector, value: Value)
  extends HasPosition with HasValue

case class Point(position: Vector, value: Value)
  extends HasPosition with HasValue

case class Obstacle(position: Vector)
  extends HasPosition

case class Empty(position: Vector) extends HasPosition

/**
 * Measures the velocity and pressure field and water
 * properties for a given position.
 */
case class Value(position: Vector,
  velocity: Vector, pressure: Double,
  density: Double, viscosity: Double) extends HasPosition {

  /**
   * Returns a copy of this with pressure modified.
   * @param p
   * @return
   */
  def modifyPressure(p: Double) =
    new Value(position, velocity, p, density, viscosity)

  /**
   * Returns a copy of this with velocity modified.
   * @param vel
   * @return
   */
  def modifyVelocity(vel: Vector) =
    new Value(position, vel, pressure, density, viscosity)

}

/**
 * Companion object.
 */
object Value {
  implicit def toValue(v: HasValue) = v.value
  def isObstacle(v: Value) = v.isInstanceOf[Obstacle]
}

/**
 * Factory for creating a solver from another.
 */
trait SolverFactory {
  def create(overrideValues: Vector => Value): Solver
}

/**
 * Companion object for `Solver`.
 */
object Solver {
  /**
   * Acceleration due to gravity. Note that this vector
   * determines the meaning of the Z direction (positive Z
   * direction is decrease in depth).
   */
  val gravity = Vector(0, 0, -9.8)

  /**
   * Returns the value of a function of interest on the Position/Value field
   */
  type PositionFunction = (Vector => Value, Vector) => Double

}

/**
 * Positions, values and methods for the numerical Navier
 * Stokes equation solver.
 */
trait Solver {
  import Solver._
  import Value._

  /**
   * ************************************************
   * Implement these
   * ************************************************
   */

  /**
   * Returns all positions.
   * @return
   */
  def getPositions: Set[Vector]

  /**
   * Returns `Value` at a position.
   * @param vector
   * @return
   */
  def getValue(position: Vector): HasValue

  /**
   * Returns a [[org.moten.david.ns.SolverFactory]] to create a new instance
   * of Solver based on this but with overriden getValue function.
   * @return
   */
  def getSolverFactory: SolverFactory

  /**
   * Returns the gradient of the function f with respect to direction at the
   * given position. `relativeTo` is used to calculate the gradient near and
   *  on boundary and obstacle positions. In particular we need to handle
   *  the case when the obstacle is of width one in a given direction and
   *  thus has non-obstacle neighbours on both sides. The proxy field values
   *  applied to the obstacle will depend the side of interest.
   * @param position
   * @param direction
   * @param f
   * @param derivativeType
   * @return
   */
  def getGradient(position: Vector, direction: Direction,
    f: PositionFunction, values: Vector => Value,
    relativeTo: Option[Vector],
    derivativeType: Derivative): Double

  /**
   * Returns calculated `Solver` after timestep seconds.
   * @param timestep
   * @return
   */
  def step(timestep: Double): Solver

  /**
   * ************************************************
   * Implemented for you
   * ************************************************
   */

  /**
   * Returns the Laplacian of the velocity vector in the given direction.
   * @param position
   * @param direction
   * @return
   */
  private def getVelocityLaplacian(position: Vector, direction: Direction) =
    getVelocityGradient2nd(position, direction).sum

  /**
   * Returns the Laplacian of the velocity vector as a vector.
   * @param position
   * @return
   */
  private def getVelocityLaplacian(position: Vector): Vector =
    Vector(getVelocityLaplacian(position, X),
      getVelocityLaplacian(position, Y),
      getVelocityLaplacian(position, Z))

  /**
   * Returns the Jacobian of velocity at a position.
   * @param position
   * @return
   */
  private def getVelocityJacobian(position: Vector) =
    Matrix(getVelocityGradient(position, X, None),
      getVelocityGradient(position, Y, None),
      getVelocityGradient(position, Z, None))

  /**
   * Returns the derivative of velocity over time using this
   * {http://en.wikipedia.org/wiki/Navier%E2%80%93Stokes_equations#Cartesian_coordinates
   *  formula}.
   * @param position
   * @return
   */
  private def dvdt(position: Vector) = {
    val value = getValue(position)
    val velocityLaplacian: Vector = getVelocityLaplacian(position)
    val pressureGradient: Vector = getPressureGradient(position)
    val velocityJacobian: Matrix = getVelocityJacobian(position)
    val divergenceOfStress =
      velocityLaplacian * value.value.viscosity minus pressureGradient
    debug("velocityLaplacian" + velocityLaplacian)
    debug("pressureGradient=" + pressureGradient)
    debug("velocityJacobian=" + velocityJacobian)
    debug("divergenceOfStress=" + divergenceOfStress)

    val result = ((divergenceOfStress) / value.density)
      .add(gravity)
      .minus(velocityJacobian * value.velocity)
    debug("dvdt=" + result)
    result
  }

  /**
   * Returns the Laplacian of pressure at position which in 3D is:
   * dp2/d2x + dp2/d2y + dp2/d2z.
   * @param position
   * @return
   */
  private def getPressureLaplacian(position: Vector) =
    getPressureGradient2nd(position).sum

  /**
   * Returns the velocity vector after time timeDelta seconds.
   * @param position
   * @param timeStep
   * @return
   */
  private def getVelocityAfterTime(position: Vector, timeDelta: Double) =
    getValue(position).value.velocity.add(dvdt(position) * timeDelta)

  /**
   * Returns the Conservation of Mass (Continuity) Equation described by the
   * Navier-Stokes equations.
   */
  private def getPressureCorrection(position: Vector, v1: Vector,
    timeDelta: Double)(pressure: Double): Double = {
    val v = getValue(position)
    //assume not obstacle or boundary
    val valueNext = v.modifyPressure(pressure)
    val solverWithOverridenPressureAtPosition =
      getSolverFactory.create(x => if (x === position) valueNext else v)
    solverWithOverridenPressureAtPosition.getPressureCorrection(position)
  }

  /**
   * Returns the value of the pressure correction function at position.
   * @param position
   * @return
   */
  private def getPressureCorrection(position: Vector): Double = {
    val value = getValue(position)
    val pressureLaplacian = getPressureLaplacian(position)
    return pressureLaplacian +
      directions.map(d => getGradient(position, d,
        gradientDot(d, Some(position)), getValue, None, FirstDerivative)).sum
  }

  /**
   * Returns the value of Del(Del dot v) for a given velocity vector v.
   * @param direction
   * @param relativeTo
   * @param v
   * @return
   */
  def gradientDot(direction: Direction,
    relativeTo: Option[Vector])(values: Vector => Value, v: Vector): Double =
    getVelocityGradient(v, direction, relativeTo) * v

  /**
   * Returns the` Value` at the given position after `timeDelta` in seconds
   * by solving
   * <a href="http://en.wikipedia.org/wiki/Navier%E2%80%93Stokes_equations#Cartesian_coordinates">
   * a 3D formulation of the Navier-Stokes equations</a>.  After the velocity
   *  calculation a pressure correction is performed according to this
   * <a href="http://en.wikipedia.org/wiki/Pressure-correction_method">method</a>.
   */
  def getValueAfterTime(position: Vector, timeDelta: Double): Value = {
    debug("getting value after time at " + position)
    val value = getValue(position)
    debug("value=" + value)
    value.value match {
      case o: Obstacle => return value
    }
    val v1 = getVelocityAfterTime(position, timeDelta)
    debug("v1=" + v1)
    val f = getPressureCorrection(position, v1, timeDelta)(_)
    //TODO what values for h,precision?
    val h = 1
    val precision = 0.000001
    val maxIterations = 15
    val newPressure = NewtonsMethod.solve(f, value.pressure, h,
      precision, maxIterations) match {
        case None => value.pressure
        case Some(a) => if (a < 0) value.pressure else a
      }
    debug("newPressure=" + newPressure + "old=" + value.pressure)
    return value.modifyPressure(newPressure).modifyVelocity(v1)
  }

  /**
   * Returns the pressure gradient vector at position.
   * @param position
   * @return
   */
  private def getPressureGradient(position: Vector): Vector =
    new Vector(directions.map(getPressureGradient(position, _)))

  /**
   * Returns the pressure gradient at position in a given direction.
   * @param position
   * @param direction
   * @return
   */
  private def getPressureGradient(position: Vector,
    direction: Direction): Double = {
    val value = getValue(position);
    getGradient(position, direction,
      (values: Vector => Value, p: Vector) => values(p).pressure,
      getValue, None, FirstDerivative)
  }

  /**
   * Returns the second derivative pressure gradient at position.
   * @param position
   * @return
   */
  private def getPressureGradient2nd(position: Vector): Vector =
    new Vector(directions.map(d =>
      getGradient(position, d,
        (values: Vector => Value, p: Vector) => values(p).pressure,
        getValue,
        None, SecondDerivative)))

  /**
   * Returns the gradient of the velocity vector at position in the given
   * direction and for the purposes of obstacle gradient calculation includes the
   * relativeTo position so a neighbour in the direction of relativeTo can be
   * chosen paired with the position of the obstacle itself for the gradient
   * calculation.
   * @param position
   * @param direction
   * @param relativeTo
   * @return
   */
  private def getVelocityGradient(position: Vector, direction: Direction,
    relativeTo: Option[Vector]): Vector =
    new Vector(directions.map(d =>
      getGradient(position, direction,
        (values: Vector => Value, p: Vector) => values(p).velocity.get(d),
        getValue,
        relativeTo, FirstDerivative)))

  /**
   * Returns the gradient of the pressure gradient at position in the
   * given direction.
   * @param position
   * @param direction
   * @return
   */
  private def getVelocityGradient2nd(position: Vector,
    direction: Direction): Vector =
    new Vector(directions.map(d =>
      getGradient(position, direction,
        (values: Vector => Value, p: Vector) =>
          values(p).velocity.get(d),
        getValue, None, SecondDerivative)))

  /**
   * Returns a new immutable Solver object representing the
   * state of the system after `timestep` seconds.
   * @param solver
   * @param timestep
   * @param numSteps
   * @return
   */
  private def step(solver: Solver, timestep: Double, numSteps: Int): Solver =
    if (numSteps == 0) return solver
    else return step(solver.step(timestep), timestep, numSteps - 1)

  /**
   * Returns a new immutable Solver object after repeating the
   *  timestep `numSteps` times.
   * @param timestep
   * @param numSteps
   * @return
   */
  def step(timestep: Double, numSteps: Int): Solver =
    step(this, timestep, numSteps)

  /**
   * Returns a readable view of the positions and their values.
   * @return
   */
  override def toString = getPositions.toList.sorted(VectorOrdering)
    .map(v => (v, getValue(v)).toString + "\n").toString
}

/**
 * Utility methods for a Grid of 3D points.
 */
object Grid {

  type DirectionalNeighbours = Map[(Direction, Double), (Option[Double], Option[Double])]

  /**
   * Returns the neighbours of an ordinate in a given direction.
   * @param vectors
   * @return
   */
  def getDirectionalNeighbours(
    vectors: Set[Vector]): DirectionalNeighbours = {
    info("getting directional neighbours")
    //produce a map of Direction to a map of ordinate values with their 
    //negative and positive direction neighbour ordinate values. This 
    //map will return None for all elements on the boundary.
    directions.map(d => {
      val b = vectors.map(_.get(d)).toSet.toList.sorted
      if (b.size < 3)
        List()
      else
        b.sliding(3).toList.map(
          x => ((d, x(1)), (Some(x(0)), Some(x(2)))))
          .++(List(((d, b(0)), (None, Some(b(1))))))
          .++(List(((d, b(b.size - 1)), (Some(b(b.size - 2)), None))))
    }).flatten.toMap
  }

  /**
   * Returns the boundary ordinates of the vectors set which is assumed
   *  to be a 3D grid.
   * @param vectors
   * @return
   */
  def getExtremes(vectors: Set[Vector]): Direction => (Double, Double) =
    directions.map(d => {
      val list = vectors.map(_.get(d)).toList
      (d, (list.min, list.max))
    }).toMap.getOrElse(_, unexpected)
}

/**
 * Regular or irregular grid of 3D points (vectors).
 */
case class Grid(positions: Set[Vector]) {
  val neighbours = Grid.getDirectionalNeighbours(positions)
}

/**
 * An enrichment of the Tuple2 api for one generic type.
 * @param <A>
 */
class RichTuple2[A](t: Tuple2[A, A]) {
  def map[B](f: A => B): Tuple2[B, B] = (f(t._1), f(t._2))
  def exists(f: A => Boolean) = f(t._1) || f(t._2)
  def find(f: A => Boolean) =
    if (f(t._1)) Some(t._1)
    else if (f(t._2)) Some(t._2) else None
}

/**
 * Implicit conversion to RichTuple.
 */
object RichTuple2 {
  implicit def toRichTuple[A](t: Tuple2[A, A]) = new RichTuple2(t)
}

object RegularGridSolver {
  import scala.math._
  import Solver._
  import RichTuple2._
  import Value._

  def getGradient(position: Vector, direction: Direction,
    n: Tuple2[Option[Double], Option[Double]],
    f: PositionFunction, values: Vector => Value,
    derivativeType: Derivative): Double = {

    val t: Tuple2[Double, Double] = n match {
      case (Some(n1), Some(n2)) => (n1, n2)
      case (None, Some(n2)) => unexpected
      case (Some(n1), None) => unexpected
      case _ => unexpected
    }
    getGradient(
      (t._1, f(values, position.modify(direction, t._1))),
      (position.get(direction), f(values, position)),
      (t._2, f(values, position.modify(direction, t._2))),
      derivativeType)
  }

  private type Pair = (Double, Double)

  private def getGradient(a1: Pair, a: Pair, a2: Pair,
    derivativeType: Derivative): Double =
    if (derivativeType equals FirstDerivative)
      (a2._2 - a1._2) / (a2._1 - a1._1)
    else
      (a2._2 + a1._2 - 2 * a._2) / (a2._1 - a1._1)

  def getGradientAtObstacle(grid: Grid, position: Vector,
    direction: Direction, f: PositionFunction, values: Vector => Value,
    relativeTo: Option[Vector], derivativeType: Derivative): Double =
    relativeTo match {
      case None => unexpected("""relativeTo must be supplied as a protected
 non-empty parameter if obstacle/boundary gradient is being calculated""")
      case Some(x) => {
        //get the neighbour in direction closest to relativeTo
        val n = getNeighbours(grid, position, direction)
        val sign = signum(x.get(direction) - position.get(direction))
        val n2 = if (sign < 0)
          (n._1, Some(position.get(direction)))
        else
          (Some(position.get(direction)), n._2)
        return RegularGridSolver.getGradient(position,
          direction, n2, f, values, derivativeType)
      }
    }

  def getNeighbours(grid: Grid, position: Vector,
    d: Direction): Tuple2[Option[Double], Option[Double]] =
    grid.neighbours.getOrElse((d, position.get(d)), unexpected)

  def getGradient(grid: Grid, position: Vector, direction: Direction,
    f: PositionFunction, values: Vector => Value, relativeTo: Option[Vector],
    derivativeType: Derivative): Double = {
    todo
  }

  def todo = throw new RuntimeException("not implemented, TODO")

  def getGradient(f: PositionFunction, v1: HasPosition, v2: HasPosition, v3: HasPosition,
    direction: Direction, relativeTo: Option[Vector],
    derivativeType: Derivative): Double = {

    type O = Obstacle
    type P = Point
    type A = HasPosition
    type B = Boundary
    type E = Empty

    //sign = 0 if no relativeTo and v2 is Point
    //sign= 1 if relativeTo is on the v3 side 
    //sign = -1 if relativeTo is on the v1 side
    val sign = getSign(v2, relativeTo, direction)

    (v1, v2, v3) match {
      case v: (P, P, P) => todo
      case v: (O, P, A) => todo
      case v: (A, P, O) => todo
      case v: (B, P, A) => todo
      case v: (A, P, B) => todo
      case v: (E, O, P) => todo
      case v: (P, O, E) => todo
      case v: (A, O, A) => todo
      case v: (A, B, A) => todo
      case _ => unexpected
    }
  }

  private def getGradient(f: Point => Double, p1: Point, p2: Point, p3: Point,
    direction: Direction, relativeTo: Option[Vector],
    derivativeType: Derivative): Double = {
    derivativeType match {
      case FirstDerivative =>
        (f(p3) - f(p1)) / (p3.position.get(direction) - p1.position.get(direction))
      case SecondDerivative =>
        (f(p3) + f(p1) - 2 * f(p2)) / (p3.position.get(direction) - p1.position.get(direction))
      case _ => unexpected
    }
  }

  private def getSign(x: HasPosition, relativeTo: Option[Vector], direction: Direction): Double = {
    relativeTo match {
      case None => if (!Point.getClass.isInstance(x))
        throw new RuntimeException("relativeTo must be specified if calculating gradient at an obstacle or boundary")
      else 0
      case Some(v: Vector) => Math.signum(x.position.get(direction) - v.get(direction))
    }
  }
}

/**
 * Implements gradient calculation for a regular grid. Every positionA,
 * on the grid has nominated neighbours to be used in gradient
 * calculations (both first and second derivatives).
 */
class RegularGridSolver(grid: Grid,
  values: Vector => Value, validate: Boolean) extends Solver {
  import Solver._
  import Grid._
  import RichTuple2._
  import scala.math._
  import RegularGridSolver._

  if (validate)
    info("validated")

  def this(positions: Set[Vector], values: Vector => Value) =
    this(Grid(positions), values, true);

  def this(map: Map[Vector, Value]) =
    this(Grid(map.keySet), map.getOrElse(_: Vector, unexpected), true)

  override def getValue(vector: Vector): Value =
    values(vector)

  override def getPositions = grid.positions

  override val getSolverFactory = new SolverFactory {
    def create(overrideValues: Vector => Value) =
      new RegularGridSolver(grid, overrideValues, validate = false)
  }

  override def getGradient(position: Vector, direction: Direction,
    f: PositionFunction, values: Vector => Value, relativeTo: Option[Vector],
    derivativeType: Derivative): Double =
    return RegularGridSolver.getGradient(grid, position, direction,
      f, values, relativeTo, derivativeType);

  override def step(timestep: Double): Solver = {
    info("creating parallel collection")
    val vectors = grid.positions //.par
    info("solving timestep")
    val stepped = vectors.map(v => (v, getValueAfterTime(v, timestep)))
    info("converting to sequential collection")
    val seq = stepped.seq
    info("converting to map")
    val newMap = seq.toMap
    info("creating new Solver")
    return new RegularGridSolver(grid,
      newMap.getOrElse(_: Vector, unexpected), false)
  }
}

/**
 * Newton's Method solver for one dimensional equations in the real numbers.
 *
 */
object NewtonsMethod {
  import scala.math._
  import scala.annotation._

  /**
   * Uses Newton's Method to solve f(x) = 0 for x. Returns `None`
   * if no solution found within maxIterations. This method uses
   * tail recursion optimisation so a large number of maxIterations
   * will not cause a stack overflow.
   *
   * @param f function to find roots of (where f(x)=0)
   * @param x initial guess at the solution.
   * @param h the delta for calculation of derivative
   * @param precision the desired maximum absolute value of f(x) at an
   *        acceptable solution
   * @param maxIterations the maximum number of iterations to perform.
   *        If maxIterations is reached then returns `None`
   * @return optional solution
   */
  @tailrec
  def solve(f: Double => Double, x: Double, h: Double,
    precision: Double, maxIterations: Long): Option[Double] = {
    val fx = f(x)
    if (abs(fx) <= precision) Some(x)
    else if (maxIterations == 0) None
    else {
      val gradient = (f(x + h) - fx) / h
      if (gradient == 0) None
      else solve(f, x - fx / gradient, h, precision, maxIterations - 1)
    }
  }
}