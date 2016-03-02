#!/usr/bin/env python
"""
Upload a file to S3
"""

from sys import argv, stdout
from time import time
from os.path import basename
from optparse import OptionParser

from boto.s3.key import Key
from boto.s3.connection import S3Connection


def uploadFile( filename, dest=None, bucket=None, overwrite=False ):
    "Upload a file to a bucket"
    if not bucket:
        bucket = 'onos'
    if not dest: 
        key = basename( filename )
    else:
        key = dest + basename( filename ) #FIXME add the /
    print '* Uploading', filename, 'to bucket', bucket, 'as', key
    stdout.flush()
    start = time()
    def callback( transmitted, size ):
        "Progress callback for set_contents_from_filename"
        elapsed = time() - start
        percent = 100.0 * transmitted / size
        kbps = .001 * transmitted / elapsed
        print ( '\r%d bytes transmitted of %d (%.2f%%),'
                ' %.2f KB/sec ' %
                ( transmitted, size, percent, kbps ) ),
        stdout.flush()
    conn = S3Connection()
    bucket = conn.get_bucket( bucket )
    k = Key( bucket )
    k.key = key
    if overwrite or not k.exists():
        k.set_contents_from_filename( filename, cb=callback, num_cb=100 )
        print
        elapsed = time() - start
        print "* elapsed time: %.2f seconds" % elapsed
    else:
        print 'file', basename( filename ), 'already exists in', bucket.name

def testAccess( bucket=None ):
    "Verify access to a bucket"
    if not bucket:
        bucket = 'onos'

    conn = S3Connection()
    bucket = conn.get_bucket( bucket )
    print bucket.get_acl()


if __name__ == '__main__':
    usage = "Usage: %prog [options] <file to upload>"
    parser = OptionParser(usage=usage)
    parser.add_option("-b", "--bucket", dest="bucket",
                      help="Bucket on S3")
    parser.add_option("-d", "--dest", dest="dest",
                      help="Destination path in bucket")
    parser.add_option("-k", "--key", dest="awsKey",
                      help="Bucket on S3")
    parser.add_option("-s", "--secret", dest="awsSecret",
                      help="Bucket on S3")
    parser.add_option("-f", "--force", dest="overwrite",
                      help="Overwrite existing file")
    parser.add_option("-v", "--verify", dest="verify",  action="store_true",
                      help="Verify access to a bucket")
    (options, args) = parser.parse_args()

    if options.verify:
        testAccess(options.bucket)
        exit()

    if len( args ) == 0:
        parser.error("missing filenames")
    for file in args:
        uploadFile( file, options.dest, options.bucket, options.overwrite )

    #FIXME key and secret are unused
