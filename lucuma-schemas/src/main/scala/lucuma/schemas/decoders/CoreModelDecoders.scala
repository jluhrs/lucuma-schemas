// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.schemas.decoders

import algebra.instances.all.given
import cats.syntax.all._
import coulomb._
import coulomb.policy.spire.standard.given
import coulomb.syntax._
import eu.timepit.refined.types.numeric.NonNegLong
import eu.timepit.refined.types.numeric.PosInt
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.generic.semiauto
import io.circe.refined._
import lucuma.core.math.Angle
import lucuma.core.math.Axis
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Epoch
import lucuma.core.math.Offset
import lucuma.core.math.Parallax
import lucuma.core.math.ProperMotion
import lucuma.core.math.RadialVelocity
import lucuma.core.math.RightAscension
import lucuma.core.math.Wavelength
import lucuma.core.math.WavelengthRange
import lucuma.core.math.dimensional._
import lucuma.core.math.units.CentimetersPerSecond
import lucuma.core.math.units.MetersPerSecond
import lucuma.core.model.NonNegDuration
import lucuma.core.optics.Format
import lucuma.core.util.*

import java.time.Duration
import java.time.temporal.ChronoUnit

trait CoreModelDecoders {

  implicit def quantityDecoder[N: Decoder, U]: Decoder[Quantity[N, U]] =
    Decoder.instance(_.as[N].map(_.withUnit[U]))

  implicit def taggedMeasureDecoder[N: Decoder, T](implicit
    unitDecoder: Decoder[Units Of T]
  ): Decoder[Measure[N] Of T] =
    Decoder.instance(c =>
      for {
        v <- c.downField("value").as[N]
        u <- c.downField("units").as[Units Of T]
      } yield u.withValueTagged(v)
    )

  implicit val epochDecoder: Decoder[Epoch] =
    Decoder.decodeString.emap(e =>
      Epoch.fromString.getOption(e).toRight(s"Invalid epoch value: $e")
    )

  private val rvmsDecoder: Decoder[RadialVelocity] =
    Decoder.decodeBigDecimal.emap(x =>
      RadialVelocity(x.withUnit[CentimetersPerSecond].toUnit[MetersPerSecond])
        .toRight(s"Invalid radial velocity $x")
    )

  implicit val rvDecoder: Decoder[RadialVelocity] =
    Decoder.instance(_.downField("centimetersPerSecond").as[RadialVelocity](rvmsDecoder))

  private val pxµasDecoder: Decoder[Parallax] =
    Decoder.decodeLong.map(Parallax.fromMicroarcseconds)

  implicit val pxDecoder: Decoder[Parallax] =
    Decoder.instance(_.downField("microarcseconds").as[Parallax](pxµasDecoder))

  implicit val angleDecoder: Decoder[Angle] = Decoder.instance(
    _.downField("microarcseconds").as[Long].map(Angle.microarcseconds.reverseGet)
  )

  implicit val raDecoder: Decoder[RightAscension] =
    Decoder.instance(
      _.as[Angle].map(
        (RightAscension.fromAngleExact.getOption _).map(_.getOrElse(RightAscension.Zero))
      )
    )

  implicit val decDecoder: Decoder[Declination] =
    Decoder
      .instance(
        _.as[Angle]
          .map(Declination.fromAngle.getOption _)
      )
      .emap(_.toRight("Invalid µarcsec value for declination"))

  implicit val coordDecoder: Decoder[Coordinates] = semiauto.deriveDecoder

  private val pmraµasDecoder: Decoder[ProperMotion.RA] =
    Decoder.decodeLong
      .map(ProperMotion.RA.microarcsecondsPerYear.get)

  implicit val pmraDecoder: Decoder[ProperMotion.RA] =
    Decoder.instance(_.downField("microarcsecondsPerYear").as[ProperMotion.RA](pmraµasDecoder))

  private val pmdecµasDecoder: Decoder[ProperMotion.Dec] =
    Decoder.decodeLong
      .map(ProperMotion.Dec.microarcsecondsPerYear.get)

  implicit val pmdecDecoder: Decoder[ProperMotion.Dec] =
    Decoder.instance(_.downField("microarcsecondsPerYear").as[ProperMotion.Dec](pmdecµasDecoder))

  implicit val pmDecoder: Decoder[ProperMotion] = semiauto.deriveDecoder[ProperMotion]

  implicit val durationDecoder: Decoder[Duration] = Decoder.instance(
    _.downField("microseconds")
      .as[Long]
      .map(l => Duration.of(l, ChronoUnit.MICROS))
  )

  implicit val nonNegDurationDecoder: Decoder[NonNegDuration] = Decoder.instance(
    _.downField("microseconds")
      .as[NonNegLong]
      .map(l => NonNegDuration.unsafeFrom(Duration.of(l.value, ChronoUnit.MICROS)))
  )

  implicit val wavelengthDecoder: Decoder[Wavelength] = Decoder.instance(
    _.downField("picometers").as[PosInt].map(Wavelength.apply)
  )

  implicit val wavelengthRangeDecoder: Decoder[WavelengthRange] = Decoder.instance(
    _.downField("picometers").as[PosInt].map(WavelengthRange.apply)
  )

  private def offsetComponentDecoder[A]: Decoder[Offset.Component[A]] = Decoder.instance(
    _.as[Angle].map(Offset.Component.apply)
  )

  implicit val offsetPComponentDecoder: Decoder[Offset.P] = offsetComponentDecoder[Axis.P]

  implicit val offsetQComponentDecoder: Decoder[Offset.Q] = offsetComponentDecoder[Axis.Q]

  implicit val offsetDecoder: Decoder[Offset] = Decoder.instance(c =>
    for {
      p <- c.downField("p").as[Offset.Component[Axis.P]]
      q <- c.downField("q").as[Offset.Component[Axis.Q]]
    } yield Offset(p, q)
  )

  // Copied from lucuma-odb for now ...
  implicit val timeSpanDecoder: Decoder[TimeSpan] =
    Decoder.instance { c =>
      def from[T: Decoder](field: String, format: Format[T, TimeSpan]): Decoder.Result[TimeSpan] =
        c.downField(field).as[T].flatMap { t =>
          format
            .getOption(t)
            .toRight(DecodingFailure(s"Invalid TimeSpan $field: $t", c.history))
        }

      c.downField("microseconds")
        .as[Long]
        .flatMap { µs =>
          TimeSpan.FromMicroseconds
            .getOption(µs)
            .toRight(DecodingFailure(s"Invalid TimeSpan microseconds: $µs", c.history))
        }
        .orElse(from("milliseconds", TimeSpan.FromMilliseconds))
        .orElse(from("seconds", TimeSpan.FromSeconds))
        .orElse(from("minutes", TimeSpan.FromMinutes))
        .orElse(from("hours", TimeSpan.FromHours))
        .orElse(from("iso", TimeSpan.FromString))
        .orElse(DecodingFailure(s"Could not parse duration value", c.history).asLeft)
    }
}
