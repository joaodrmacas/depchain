// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";

contract Blacklist is Ownable {
    mapping(address => bool) private _blacklist;

    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    constructor() Ownable(msg.sender) {}

    function addToBlacklist(address account) external onlyOwner {
        require(account != address(0), "Invalid address");
        require(!_blacklist[account], "Address already blacklisted");
        
        _blacklist[account] = true;
        emit AddedToBlacklist(account);
    }

    function removeFromBlacklist(address account) external onlyOwner {
        require(account != address(0), "Invalid address");
        require(_blacklist[account], "Address not blacklisted");
        
        _blacklist[account] = false;
        emit RemovedFromBlacklist(account);
    }

    function isBlacklisted(address account) public view returns (bool) {
        return _blacklist[account];
    }
}