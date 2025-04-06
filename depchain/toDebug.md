# Blockchain Client Commands Status

## Native Blockchain Commands
| Command | Status | Format |
|---------|--------|--------|
| **TRANSFER_DEPCOIN** | ✅ WORKING | `<to_id> <amount>` |
| **BALANCEOF_DEPCOIN** | ✅ WORKING | `<client_id>` |

## ISTContract Functions
| Command | Status | Format |
|---------|--------|--------|
| **balanceOf** | ✅ WORKING | `[<account_id>]` |
| **transfer** | ✅ WORKING | `<to_id> <amount>` |
| **transferFrom** | ✅ WORKING | `<from_id> <to_id> <amount>` |
| **approve** | ✅ WORKING | `<spender_id> <amount>` |
| **allowance** | ✅ WORKING | `<owner_id> [<spender_id>]` |
| **isBlacklisted** | ✅ WORKING | `[<account_id>]` |
| **addToBlacklist** | ✅ WORKING | `<account_id>` |
| **removeFromBlacklist** | ✅ WORKING | `<account_id>` |
| **buy** | ✅ WORKING | `<amount>` |

## Administrative Commands
| Command | Status | Format |
|---------|--------|--------|
| **EXIT** | ✅ WORKING | - |

*Note: Square brackets `[]` indicate optional parameters*
*Note: everything working. No debug needed.*