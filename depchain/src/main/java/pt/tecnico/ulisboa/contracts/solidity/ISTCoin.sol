// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./Blacklist.sol";

contract ISTCoin is ERC20, Blacklist {
    // Exchange rate: tokens per ETH
    uint256 public tokenPricePerEth = 230000; // 1 ETH = 230000 IST tokens

    event TokensPurchased(address indexed buyer, uint256 ethAmount, uint256 tokenAmount);

    constructor() ERC20("IST Coin", "IST") {
        // Initial token supply of 100 million with 2 decimals
        uint256 initialSupply = 100_000_000 * (10 ** 2);
        _mint(msg.sender, initialSupply);
    }

    // Override decimals to set to 2
    function decimals() public view virtual override returns (uint8) {
        return 2;
    }

    // Override transfer to add blacklist check
    function transfer(address to, uint256 value) public virtual override returns (bool) {
        require(!isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!isBlacklisted(to), "Recipient is blacklisted");

        return super.transfer(to, value);
    }

    // Override transferFrom to add blacklist check
    function transferFrom(address from, address to, uint256 value) public virtual override returns (bool) {
        require(!isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!isBlacklisted(from), "From address is blacklisted");
        require(!isBlacklisted(to), "Recipient is blacklisted");

        return super.transferFrom(from, to, value);
    }

    // Override approve to add blacklist check
    function approve(address spender, uint256 value) public virtual override returns (bool) {
        require(!isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!isBlacklisted(spender), "Spender is blacklisted");

        return super.approve(spender, value);
    }

    // Function to buy IST tokens with ETH
    function buy() external payable {
        require(msg.sender != owner(), "Owner cannot buy tokens");
        require(msg.value > 0, "Must send ETH to buy tokens");
        require(!isBlacklisted(msg.sender), "Buyer is blacklisted");
        
        // Calculate token amount based on ETH sent and the token price
        uint256 tokenAmount = msg.value * tokenPricePerEth;
        
        // Adjust for decimals (IST has 2 decimals)
        tokenAmount = tokenAmount * (10 ** decimals()) / (10 ** 18);
        
        // Check if owner has enough tokens
        address ownerAddress = owner();
        require(balanceOf(ownerAddress) >= tokenAmount, "Not enough tokens available for sale");
        
        // Directly transfer tokens from owner to buyer
        // This uses the internal _transfer function which doesn't require approval
        _transfer(ownerAddress, msg.sender, tokenAmount);
        
        // Transfer ETH to the owner
        (bool sent, ) = payable(ownerAddress).call{value: msg.value}("");
        require(sent, "Failed to send ETH to owner");
        
        emit TokensPurchased(msg.sender, msg.value, tokenAmount);
    }
}