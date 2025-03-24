// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/access/Ownable.sol";

contract Blacklist is Ownable {
    mapping(address => bool) private _blacklist;

    // Event for logging blacklist changes
    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    constructor() Ownable(msg.sender) {}

    // Add an address to the blacklist
    function addToBlacklist(address account) external onlyOwner {
        require(account != address(0), "Invalid address");
        require(!_blacklist[account], "Address already blacklisted");
        
        _blacklist[account] = true;
        emit AddedToBlacklist(account);
    }

    // Remove an address from the blacklist
    function removeFromBlacklist(address account) external onlyOwner {
        require(account != address(0), "Invalid address");
        require(_blacklist[account], "Address not blacklisted");
        
        _blacklist[account] = false;
        emit RemovedFromBlacklist(account);
    }

    // Check if an address is blacklisted
    function isBlacklisted(address account) external view returns (bool) {
        return _blacklist[account];
    }
}
