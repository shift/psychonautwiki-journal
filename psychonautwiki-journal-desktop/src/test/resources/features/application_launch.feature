Feature: Application Launch
  As a user
  I want to launch the PsychonautWiki Journal desktop application
  So that I can track my substance experiences safely

  Scenario: Application starts successfully
    Given the application is not running
    When I launch the application
    Then the main window should be displayed
    And the window title should be "PsychonautWiki Journal"
    And the application should show the welcome message

  Scenario: Database initialization
    Given the application is starting for the first time
    When the application initializes
    Then a SQLite database should be created
    And the database schema should be applied
    And the user preferences table should exist