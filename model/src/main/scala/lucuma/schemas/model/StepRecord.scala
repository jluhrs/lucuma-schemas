// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.schemas.model

import cats.Eq
import cats.derived.*
import lucuma.core.enums.StepQaState
import lucuma.core.model.sequence.Step
import lucuma.core.model.sequence.StepConfig
import lucuma.core.model.sequence.gmos.DynamicConfig
import lucuma.core.util.TimeSpan
import org.typelevel.cats.time.given

import java.time.Instant

sealed trait StepRecord derives Eq:
  def id: Step.Id
  def created: Instant
  def startTime: Option[Instant]
  def endTime: Option[Instant]
  def duration: Option[TimeSpan]
  def instrumentConfig: DynamicConfig
  def stepConfig: StepConfig
  def stepEvents: List[StepEvent]
  def stepQaState: Option[StepQaState]
  def datasetEvents: List[DatasetEvent]
  def datasets: List[Dataset]

object StepRecord:
  case class GmosNorth protected[schemas] (
    id:               Step.Id,
    created:          Instant,
    startTime:        Option[Instant],
    endTime:          Option[Instant],
    duration:         Option[TimeSpan],
    instrumentConfig: DynamicConfig.GmosNorth,
    stepConfig:       StepConfig,
    stepEvents:       List[StepEvent],
    stepQaState:      Option[StepQaState],
    datasetEvents:    List[DatasetEvent],
    datasets:         List[Dataset]
  ) extends StepRecord
      derives Eq

  case class GmosSouth protected[schemas] (
    id:               Step.Id,
    created:          Instant,
    startTime:        Option[Instant],
    endTime:          Option[Instant],
    duration:         Option[TimeSpan],
    instrumentConfig: DynamicConfig.GmosSouth,
    stepConfig:       StepConfig,
    stepEvents:       List[StepEvent],
    stepQaState:      Option[StepQaState],
    datasetEvents:    List[DatasetEvent],
    datasets:         List[Dataset]
  ) extends StepRecord
      derives Eq
