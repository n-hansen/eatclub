module Eatclub.Queries exposing (..)

import Time.Date exposing (Date)
import Time.Date as Date
import Time.DateTime exposing (DateTime)
import Time.DateTime as DateTime
import GraphQL.Request.Builder exposing (Request, ValueSpec, Document, Query)
import GraphQL.Request.Builder as Req
import GraphQL.Request.Builder.Arg as Arg
import GraphQL.Request.Builder.Variable exposing (VariableSpec)
import GraphQL.Request.Builder.Variable as Var
import GraphQL.Client.Http as GraphQLClient
import Json.Decode as Decode

type alias Id = String

type alias Menu =
    { date : Date
    , listed_items : List ItemListing
    }

type alias ItemListing =
    { item : Item
    , snapshots : List Snapshot
    }

type alias Item =
    { id : Id
    , name : String
    }

type alias Snapshot =
    { timestamp : DateTime
    , quantity : Int
    , hidden : Bool
    , average_rating : Float
    , review_count : Int
    }

type DateType = DateType

dateVal : ValueSpec Req.NonNull DateType Date vars
dateVal =
    Decode.string
        |> Decode.andThen
           (\s ->
                case Date.fromISO8601 s of
                    Ok date -> Decode.succeed date
                    Err msg -> Decode.fail msg)
        |> Req.customScalar DateType


type Aggregation = AggFirst
                 | AggLast
                 | AggMinimum
                 | AggMaximum
                 | AggMean

dateVar : VariableSpec Var.NonNull Date
dateVar =
    Var.enum "date" Date.toISO8601

type TimestampType = TimestampType

timestampVal : ValueSpec Req.NonNull TimestampType DateTime vars
timestampVal =
    Decode.string
        |> Decode.andThen
           (\s ->
                case DateTime.fromISO8601 s of
                    Ok ts -> Decode.succeed ts
                    Err msg -> Decode.fail msg)
        |> Req.customScalar TimestampType


aggregationVar : VariableSpec Var.NonNull Aggregation
aggregationVar =
    let
        serialize agg =
            case agg of
                AggFirst -> "first"
                AggLast -> "last"
                AggMinimum -> "minimum"
                AggMaximum -> "maximum"
                AggMean -> "mean"
    in
        Var.enum "aggregation" serialize

type alias RequestSpec =
    { menuDate : Date
    , quantityAgg : Aggregation
    , hiddenAgg : Aggregation
    , ratingAgg : Aggregation
    , reviewCountAgg : Aggregation
    }

menuQuery : Document Query Menu RequestSpec
menuQuery =
    let
        quantityAgg =
            Var.required "quantityAgg" .quantityAgg aggregationVar
                |> Arg.variable

        hiddenAgg =
            Var.required "hiddenAgg" .hiddenAgg aggregationVar
                |> Arg.variable

        ratingAgg =
            Var.required "ratingAgg" .ratingAgg aggregationVar
                |> Arg.variable

        reviewCountAgg =
            Var.required "reviewCountAgg" .reviewCountAgg aggregationVar
                |> Arg.variable

        menuDate =
            Var.required "menuDate" .menuDate dateVar
                |> Arg.variable

        menu =
            Req.object Menu
                |> Req.with (Req.field "date" [] dateVal)
                |> Req.with (Req.field "listed_items" [] (Req.list itemListing))

        itemListing =
            Req.object ItemListing
                |> Req.with (Req.field "item" [] item)
                |> Req.with (Req.field "snapshots" [] (Req.list snapshot))

        item =
            Req.object Item
                |> Req.with (Req.field "id" [] Req.id)
                |> Req.with (Req.field "name" [] Req.string)

        snapshot =
            Req.object Snapshot
                |> Req.with (Req.field "timestamp" [] timestampVal)
                |> Req.with (Req.field "quantity" [("aggregation", quantityAgg)] Req.int)
                |> Req.with (Req.field "hidden" [("aggregation", hiddenAgg)] Req.bool)
                |> Req.with (Req.field "average_rating" [("aggregation", ratingAgg)] Req.float)
                |> Req.with (Req.field "review_count" [("aggregation", reviewCountAgg)] Req.int)

        queryRoot =
            Req.extract
                (Req.field "menu"
                     [("date", menuDate)]
                     menu)

    in
        Req.queryDocument queryRoot

defaultRequest : RequestSpec
defaultRequest =
    { menuDate = Date.date 2018 07 02
    , quantityAgg = AggLast
    , hiddenAgg = AggMaximum
    , ratingAgg = AggMean
    , reviewCountAgg = AggMaximum
    }

menuRequest : RequestSpec -> Request Query Menu
menuRequest rs =
    menuQuery
        |> Req.request rs
