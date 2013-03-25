This plugin adds a Job Priority field to each Hudson job.  Jobs with higher
priorities will be ran before jobs with lower priorities when there are jobs
waiting in the build queue.  This can be very helpful when one wants to add
low priority jobs but wants to have higher-priority jobs run first when
hardware is limited.

=============

Maintainer

Brad Larson - bklarson@gmail.com


-------------

TODO

This fork was created by Rob Vesse to attempt to extend the plugin with some/all of 
the following functionality:

- Support multiple prioritization criteria e.g.
 - Run faster builds first
 - Run recently failed builds first
- Address some reported bugs with the plugin that appear trivial
 - JENKINS-16247 (DONE)
