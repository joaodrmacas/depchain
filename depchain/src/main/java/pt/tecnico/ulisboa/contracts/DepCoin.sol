// SPDX-License-Identifier: MIT
// OpenZeppelin Contracts (last updated v5.2.0) (token/ERC20/ERC20.sol)

pragma solidity ^0.8.20;

import "./ERC20.sol";

contract ISTCoin is ERC20 {

    Blacklist private _blacklist;

    constructor() ERC20("IST Coin", "IST") {}

    function decimals() public view virtual override returns (uint8) {
        return 2;
    }

    /**
     * @dev Override transfer to add blacklist check
     */
    function transfer(address to, uint256 value) public virtual override returns (bool) {
        // Check if the sender is allowed to transfer
        require(!_blacklist.isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!_blacklist.isBlacklisted(to), "Recipient is blacklisted");

        return super.transfer(to, value);
    }

    /**
     * @dev Override transferFrom to add blacklist check
     */
    function transferFrom(address from, address to, uint256 value) public virtual override returns (bool) {
        // Check if the sender, from, and to addresses are not blacklisted
        require(!_blacklist.isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!_blacklist.isBlacklisted(from), "From address is blacklisted");
        require(!_blacklist.isBlacklisted(to), "Recipient is blacklisted");

        return super.transferFrom(from, to, value);
    }

    /**
     * @dev Override approve to add blacklist check
     */
    function approve(address spender, uint256 value) public virtual override returns (bool) {
        // Check if the sender and spender are not blacklisted
        require(!_blacklist.isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!_blacklist.isBlacklisted(spender), "Spender is blacklisted");

        return super.approve(spender, value);
    }

    
}