# Price-curve changes and the purchase ledger

BuyClaimChunks Continued records the item amount actually consumed by successful purchases. This lets a server operator change the price curve later without removing land or claim capacity that players already own.

## Stored values

The server stores one account per player UUID in world `SavedData`:

```text
currency item registry ID
paid claim-capacity count
lifetime consumed item count
schema version
```

FTB Chunks or OpenPAC remains the source of truth for the player's current total extra capacity. The BuyClaimChunks ledger records only the economic origin of capacity bought through this mod.

## Increased prices

Existing claims are never removed.

After a price increase, the next purchase pays the difference between the new cumulative curve and the amount already consumed:

```text
next payment
= cumulative cost of all paid claims after this purchase under the new curve
- lifetime consumed amount
```

Example:

```text
2 paid claims
lifetime consumed: 2 diamonds
new curve: fixed 4 diamonds per claim
new cumulative cost through claim 2: 8
carried debt: 6
claim 3 normal price: 4
next purchase price: 10
```

The player keeps both existing claims. The missing 6 diamonds are recovered only when another claim is purchased.

## Decreased prices

When previous payments buy more claims under the new curve, the difference becomes claim credit.

On the next successful `/buyclaim` transaction, the mod grants compensation claims before the requested purchase. The total number granted by one command, including the requested amount, is bounded by `maxPurchaseAmount`; the backend total is also bounded by `maxExtraClaims`. Credit that cannot be granted in that transaction remains represented by the lifetime payment total and can make later claims free or cheaper.

Example:

```text
2 paid claims
lifetime consumed: 12 diamonds
new curve: fixed 4 diamonds per claim
12 diamonds now buy 3 claims
next /buyclaim grants 1 compensation claim
and buys the requested next claim for 4 diamonds
```

## Currency changes

Changing only `amountRequired`, `priceGrowthFactor`, or `priceExponent` preserves exact historical spending and triggers debt or credit.

Changing `itemRequired` cannot be converted automatically because the server has no universal exchange rate between, for example, diamonds and a modded coin. A currency change therefore starts a new baseline at the same paid-claim count under the active curve. It does not create debt or credit between different item types.

## Legacy worlds

Versions before the purchase ledger did not store lifetime consumed items. On first use after upgrading, the current backend extra capacity is treated as already paid at the active curve. This preserves the next-price behavior and starts exact tracking from that point onward.

Because old backend capacity cannot distinguish purchases from administrator grants, operators should review unusual accounts before changing the curve immediately after upgrading.

## Transaction order

1. Read backend capacity and the payment ledger.
2. Calculate carried debt or compensation credit.
3. Validate inventory payment.
4. Update and verify backend capacity.
5. Compare-and-set the ledger to the quoted result.
6. Consume payment.
7. If payment unexpectedly fails, roll back both backend capacity and the ledger.

A rejected transaction consumes no items.
