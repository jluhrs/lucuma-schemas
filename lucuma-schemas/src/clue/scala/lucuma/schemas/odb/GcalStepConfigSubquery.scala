// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.schemas.odb

import clue.GraphQLSubquery
import clue.annotation.*
import lucuma.schemas.ObservationDB

@GraphQL
abstract class GcalStepConfigSubquery extends GraphQLSubquery[ObservationDB]("Gcal"):
  override val subquery: String = """
        {
          continuum
          arcs
          filter
          diffuser
          shutter
        }
      """

@GraphQLStub
object GcalStepConfigSubquery