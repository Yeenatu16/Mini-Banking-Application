package com.banking;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class Bank {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Replace with your MySQL password
        DBConnection db = new DBConnection(
                "jdbc:mysql://localhost:3306/bank",
                "root",
                "***********"
        );
        TransactionLogger tranLogger = new TransactionLogger(db);
        BankManagement bm = new BankManagement(db, tranLogger);

        while (true) {
            System.out.println("\n======= WELCOME TO OUR BANK =======");
            System.out.println("1) Create Account");
            System.out.println("2) Login & Manage Account");
            System.out.println("3) Exit");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> {
                    System.out.print("Username: ");
                    String uname = sc.nextLine();
                    System.out.print("Password: ");
                    String passCode = sc.nextLine();

                    BigDecimal deposit;
                    try {
                        System.out.print("Initial deposit: ");
                        deposit = new BigDecimal(sc.nextLine());
                        if (deposit.compareTo(BigDecimal.ZERO) <= 0) {
                            System.out.println("❌ Deposit must be greater than 0.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Invalid deposit amount.");
                        break;
                    }

                    String acNo = bm.createAccount(uname, passCode, deposit);
                    if (acNo != null) {
                        System.out.println("✅ Account created: " + acNo);
                        System.out.println("💡 Keep this account number safe for login.");
                    } else {
                        System.out.println("❌ Failed to create account.");
                    }
                }

                case "2" -> {
                    System.out.print("Enter Account Number: ");
                    String formattedAcNo = sc.nextLine();
                    System.out.print("Enter Password: ");
                    String password = sc.nextLine();

                    String acNo = bm.login(formattedAcNo, password);
                    if (acNo == null)
                        break;

                    System.out.println("✅ Login successful!");

                    boolean loggedIn = true;
                    while (loggedIn) {
                        System.out.println("\n1) Deposit  2) Withdraw  3) Transfer  4) Check Balance  5) Transaction History  6) Logout");
                        System.out.print("Choice: ");
                        String op = sc.nextLine();

                        switch (op) {
                            case "1" -> {
                                try {
                                    System.out.print("Amount: ");
                                    BigDecimal amt = new BigDecimal(sc.nextLine());
                                    bm.deposit(acNo, amt, "user:" + acNo);
                                } catch (NumberFormatException e) {
                                    System.out.println("❌ Invalid amount.");
                                }
                            }
                            case "2" -> {
                                try {
                                    System.out.print("Amount: ");
                                    BigDecimal amt = new BigDecimal(sc.nextLine());
                                    bm.withdraw(acNo, amt, "user:" + acNo);
                                } catch (NumberFormatException e) {
                                    System.out.println("❌ Invalid amount.");
                                }
                            }
                            case "3" -> {
                                System.out.print("To Account #: ");
                                String toAcNo = sc.nextLine();
                                try {
                                    System.out.print("Amount: ");
                                    BigDecimal amt = new BigDecimal(sc.nextLine());
                                    bm.transfer(acNo, toAcNo, amt, "user:" + acNo);
                                } catch (NumberFormatException e) {
                                    System.out.println("❌ Invalid amount.");
                                }
                            }
                            case "4" -> {
                                BigDecimal bal = bm.getBalance(acNo);
                                if (bal != null) System.out.println("Balance: " + bal);
                            }
                            case "5" -> {
                                List<String> history = bm.getTransactionHistory(acNo);
                                if (history.isEmpty()) System.out.println("No transactions found.");
                                else history.forEach(System.out::println);
                            }
                            case "6" -> loggedIn = false;
                            default -> System.out.println("Invalid choice");
                        }
                    }
                }

                case "3" -> {
                    System.out.println("Goodbye!");
                    return;
                }

                default -> System.out.println("Invalid choice");
            }
        }
    }
}
