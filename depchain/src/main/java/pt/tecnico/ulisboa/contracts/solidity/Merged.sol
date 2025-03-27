// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract Merged is ERC20, Ownable {
    // Blacklist mapping
    mapping(address => bool) private _blacklist;

    // Events for blacklist changes
    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    constructor() ERC20("IST Coin", "IST") Ownable(msg.sender) {
        // Initial token supply of 100 million with 2 decimals
        uint256 initialSupply = 100_000_000 * (10 ** 2);
        _mint(msg.sender, initialSupply);
    }

    // Override decimals to set to 2
    function decimals() public view virtual override returns (uint8) {
        return 2;
    }

    // Blacklist Management Functions
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

    function isBlacklisted(address account) external view returns (bool) {
        return _blacklist[account];
    }

    // Override transfer to add blacklist check
    function transfer(address to, uint256 value) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[to], "Recipient is blacklisted");

        return super.transfer(to, value);
    }

    // Override transferFrom to add blacklist check
    function transferFrom(address from, address to, uint256 value) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[from], "From address is blacklisted");
        require(!_blacklist[to], "Recipient is blacklisted");

        return super.transferFrom(from, to, value);
    }

    // Override approve to add blacklist check
    function approve(address spender, uint256 value) public virtual override returns (bool) {
        require(!_blacklist[msg.sender], "Sender is blacklisted");
        require(!_blacklist[spender], "Spender is blacklisted");

        return super.approve(spender, value);
    }
}