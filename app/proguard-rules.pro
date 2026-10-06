# Project-specific R8 rules.
#
# None are needed today: the app uses no reflection or serialization, custom
# views referenced from layouts are kept by AAPT-generated rules, and AndroidX
# and coroutines ship their own consumer rules. Add targeted -keep rules here
# only if a release build breaks.
