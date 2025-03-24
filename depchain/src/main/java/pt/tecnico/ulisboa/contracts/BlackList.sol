// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/access/Ownable.sol";

contract Blacklist is Ownable {
    mapping(address => bool) private _blacklist;

    // Add an address to the blacklist
    function addToBlacklist(address account) external onlyOwner returns (bool) {
        require(account != address(0), "Invalid address");
        _blacklist[account] = true;
        return true;
    }

    // Remove an address from the blacklist
    function removeFromBlacklist(address account) external onlyOwner returns (bool) {
        require(account != address(0), "Invalid address");
        _blacklist[account] = false;
        return true;
    }

    // Check if an address is blacklisted
    function isBlacklisted(address account) external view returns (bool) {
        return _blacklist[account];
    }
}
