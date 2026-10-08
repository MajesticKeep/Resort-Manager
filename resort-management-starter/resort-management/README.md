# Resort Management System — starter code

Generated from the project's planning docs (proposal, UML diagram, flowcharts).
Everything here compiles, but a lot of method bodies are intentionally left
as TODOs — those are the parts that need an actual design decision from
your group, not something to guess at for you.

## What's fully done
- All field definitions, constructors, getters/setters
- The Room hierarchy (abstract Room + 5 subclasses) and RoomStatus enum
- BookingStatus and AddOnCategory enums
- Class structure and relationships matching the UML diagram

## What's stubbed with TODOs — needs your group's input
- `Room.isAvailable()` — can't check for real overlaps until there's a way
  to look up a room's bookings (e.g. Booking keeping a static list, or a
  BookingManager/repository class your group hasn't designed yet)
- `Booking.calculateTotal()` — the actual rate * nights + add-ons math
- `Bill.generateBill()` — same, plus deciding how Bill pulls data from Booking
- `Staff.assignTask()` — the actual task-creation logic
- Each room subclass's `calculateRate()` — currently all just return the
  base rate; only becomes real polymorphism once/if per-type pricing rules
  are decided

## Deliberately NOT included
- `Employee` / `Admin` / `FrontDeskUser` — the login/role split is still an
  open question. Building this now risks throwaway work.
- Any Swing UI code — `resort/ui/` is empty on purpose.
- `resortapp/` package — this was the duplicate entry point from your
  original tree. Deleted. `resort/Main.java` is now the one entry point.

## Before anyone builds on this
Lock down two things with your group first:
1. Do Housekeeping/Maintenance actually extend Staff? (assumed yes here,
   flagged in both files)
2. Does the app need real login accounts at all?

Everything else in this zip is safe to build on immediately.
